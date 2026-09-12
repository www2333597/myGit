# 文件导读：用真实 HTTP 与 Cookie 演示完整考试流程；执行会新增答卷，只适合未改动题库的本机演示库。
# 本脚本和内存 H2 单元/集成测试不同，不应为了阅读讲解就自动运行。
param([string]$BaseUrl = 'http://127.0.0.1:3636')
$ErrorActionPreference = 'Stop'
# 只允许本机后端地址；它检查的是 API 主机，不会判断后端实际连接了哪个 MySQL 数据库。
$baseUri = [Uri]$BaseUrl
if ($baseUri.Host -notin @('localhost', '127.0.0.1')) {
    throw 'This demo-data smoke test only supports a local backend.'
}
$script:requestCount = 0
# 通用 HTTP 适配：保持 Cookie，序列化 UTF-8 JSON，按预期状态判断成功或预期失败。
function Send-Request($Session, [string]$Method, [string]$Path, $Body, [string]$Token, [int]$Expected = 200) {
    $script:requestCount++
    $parameters = @{
        Uri = $BaseUrl.TrimEnd('/') + $Path
        Method = $Method
        WebSession = $Session
        UseBasicParsing = $true
        TimeoutSec = 10
        ContentType = 'application/json; charset=utf-8'
    }
    # GET 不需要令牌；所有写操作传入登录后的新 Token，Session 负责保存 Cookie。
    if ($Token) { $parameters.Headers = @{ 'X-CSRF-Token' = $Token } }
    if ($null -ne $Body) {
        $parameters.Body = [Text.Encoding]::UTF8.GetBytes(($Body | ConvertTo-Json -Depth 12 -Compress))
    }
    try {
        $response = Invoke-WebRequest @parameters
    } catch {
        # PowerShell 会把 4xx/5xx 当异常；若正是预期的 401/403 就把它视为负向用例通过。
        # 这里仅验证 HTTP 状态，不解析失败响应体；详细业务码断言在 Java 集成测试中。
        if ($null -eq $_.Exception.Response) { throw }
        $actualStatus = [int]$_.Exception.Response.StatusCode
        if ($actualStatus -eq $Expected) { return $null }
        throw "Unexpected HTTP status: $Method $Path expected $Expected, received $actualStatus"
    }
    # Expected=0 只用于允许开考返回 200 或 201 的成功路径；HTTP 错误仍会在 catch 中抛出。
    if ($Expected -ne 0 -and [int]$response.StatusCode -ne $Expected) {
        throw "Unexpected HTTP status for $Method $Path"
    }
    return $response.Content | ConvertFrom-Json
}
# 每个演示身份使用独立 Cookie 容器，先取 CSRF 再登录，并保存登录响应的新 Token。
function Login-Demo([string]$Username) {
    $session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
    $initial = Send-Request $session GET '/api/auth/csrf' $null '' 200
    $login = Send-Request $session POST '/api/auth/login' @{ username = $Username; password = 'Exam@2026!' } $initial.data.csrfToken 200
    return @{ Session = $session; Token = $login.data.csrfToken; Role = $login.data.role }
}
# 阶段 1：检查应用启动、学生认证、资料字段、选课概览；UP 只是应用健康，登录才会实际查库。
$anonymous = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$health = Send-Request $anonymous GET '/api/health' $null '' 200
if ($health.data.status -ne 'UP') { throw 'Health check failed.' }
$student = Login-Demo 'student01'
if ($student.Role -ne 'STUDENT') { throw 'Student role mismatch.' }
$profile = Send-Request $student.Session GET '/api/me' $null '' 200
if ($profile.data.PSObject.Properties.Name -contains 'passwordHash') { throw 'Sensitive profile field leaked.' }
$courses = Send-Request $student.Session GET '/api/student/courses' $null '' 200
$course = @($courses.data | Where-Object { $_.courseId -eq 1 })[0]
# 已完成考试则新建重考，未完成则首次开考或复用进行中答卷。
$mode = if ($course.examStatus -eq 'COMPLETED') { 'RETAKE' } else { 'FIRST' }
$started = Send-Request $student.Session POST '/api/student/courses/1/attempts' @{ mode = $mode } $student.Token 0
$firstId = $started.data.attemptId
$paper = Send-Request $student.Session GET "/api/student/attempts/$firstId" $null '' 200
# 固定演示题库假设：Java 两题满分 30；一旦教师改题，本脚本应停下而不是按旧答案继续提交。
if ($paper.data.items.Count -ne 2 -or $paper.data.totalScore -ne 30) {
    throw 'This smoke test requires the unmodified demo question set.'
}
if (($paper | ConvertTo-Json -Depth 12) -match 'correctOption|passwordHash') { throw 'Correct answer leaked.' }
# 阶段 2：从本张卷读取真实 itemId，按种子题序构造全对答案；不是通过学生 API 获取正确答案。
$answers = @($paper.data.items | ForEach-Object {
    @{ itemId = $_.itemId; selectedOption = if ($_.positionNo -eq 1) { 'A' } else { 'B' } }
})
$submitted = Send-Request $student.Session POST "/api/student/attempts/$firstId/submit" @{ answers = $answers } $student.Token 200
if ($submitted.data.score -ne 30) { throw 'First score mismatch.' }
# 故意第二次提交空答案，验证服务器仍返回第一次成绩和时间，不能借重试改分。
$duplicate = Send-Request $student.Session POST "/api/student/attempts/$firstId/submit" @{ answers = @() } $student.Token 200
if ($duplicate.data.score -ne 30 -or $duplicate.data.submittedAt -ne $submitted.data.submittedAt) {
    throw 'Duplicate submission changed the saved result.'
}
$result = Send-Request $student.Session GET "/api/student/attempts/$firstId/result" $null '' 200
if ($result.data.score -ne 30) { throw 'Result lookup mismatch.' }
# 阶段 3：重考必须新建卷；全部漏答得 0 分，旧成绩继续存在。
$retake = Send-Request $student.Session POST '/api/student/courses/1/attempts' @{ mode = 'RETAKE' } $student.Token 201
$secondId = $retake.data.attemptId
if ($secondId -eq $firstId) { throw 'Retake reused the old attempt.' }
$retakeResult = Send-Request $student.Session POST "/api/student/attempts/$secondId/submit" @{ answers = @() } $student.Token 200
if ($retakeResult.data.score -ne 0) { throw 'Retake score mismatch.' }
# 阶段 4：教师最近成绩应指向重考记录，历史至少两次；学生访问教师路径仍应被拒绝。
$teacher = Login-Demo 'teacher01'
$grades = Send-Request $teacher.Session GET '/api/teacher/courses/1/grades' $null '' 200
if (@($grades.data.items | Where-Object { $_.studentId -eq 1001 -and $_.attemptId -eq $secondId }).Count -ne 1) {
    throw 'Teacher latest-grade lookup failed.'
}
$history = Send-Request $teacher.Session GET '/api/teacher/courses/1/students/1001/attempts' $null '' 200
if ($history.data.total -lt 2) { throw 'Previous result was lost.' }
$null = Send-Request $student.Session GET '/api/teacher/courses' $null '' 403
$null = Send-Request $student.Session POST '/api/auth/logout' $null $student.Token 200
$null = Send-Request $student.Session GET '/api/me' $null '' 401
# 阶段 5：退出后查资料为 401，最终汇总本次真实请求数量和关键结果，不包含 Cookie/数据库密码。
$summary = [ordered]@{
    passed = $true
    transport = 'Real HTTP with Cookie session'
    verifiedAt = [DateTimeOffset]::Now.ToString('o')
    requests = $script:requestCount
    firstScore = $submitted.data.score
    totalScore = $submitted.data.totalScore
    retakeScore = $retakeResult.data.score
    historyCount = $history.data.total
    duplicateSubmissionStable = $true
    roleIsolation = $true
    logoutInvalidatesSession = $true
}
# 报告写到 backend/target（Git 忽略的构建目录）；只是验证证据，不是 Apifox 云端案例保存。
$projectDirectory = Split-Path -Parent $PSScriptRoot
$reportDirectory = Join-Path $projectDirectory 'backend\target'
New-Item -ItemType Directory -Path $reportDirectory -Force | Out-Null
$reportJson = $summary | ConvertTo-Json
[IO.File]::WriteAllText((Join-Path $reportDirectory 'smoke-result.json'), $reportJson, (New-Object Text.UTF8Encoding($false)))
$reportJson
