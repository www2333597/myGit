# 文件导读：本机后端启动脚本；只为 Java 进程设置连接参数并启动已有 JAR，不执行建表或种子 SQL。
param(
    # 当前学习环境沿用已经初始化好的 exam_system_fullstack；不会自动建表或导入数据。
    [string]$DatabaseUser = 'root',
    [string]$DatabaseUrl = 'jdbc:mysql://127.0.0.1:3306/exam_system_fullstack?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true',
    # 参数校验提前拒绝明显无效端口；3636 是 HTTP 服务，MySQL 仍用连接 URL 中的 3306。
    [ValidateRange(1024, 65535)][int]$Port = 3636
)
# 遇到命令错误立即中断，避免配置失败之后继续假装启动成功。
$ErrorActionPreference = 'Stop'
# 以脚本所在目录推导项目位置，不依赖用户从哪个终端目录调用。
$projectDirectory = Split-Path -Parent $PSScriptRoot
$backendDirectory = Join-Path $projectDirectory 'backend'
$jarPath = Join-Path $backendDirectory 'target\exam-system-0.1.0.jar'
if (-not (Test-Path -LiteralPath $jarPath)) {
    throw 'Build the backend first: cd backend; mvn package'
}
# SecureString 隐藏密码输入；实际 JDBC 连接时仍需在 Java 进程内得到密码，不会写入项目文件。
$secret = Read-Host 'MySQL password (not the demo account password)' -AsSecureString
# 记录调用前的进程级变量，退出时恢复；不修改系统级或用户级环境配置。
$previousValues = @{}
foreach ($name in @('DB_USERNAME', 'DB_PASSWORD', 'DB_URL', 'SERVER_PORT')) {
    $previousValues[$name] = [Environment]::GetEnvironmentVariable($name, 'Process')
}
try {
    # Spring Boot 在 application.yml 的占位符中读取这些变量；子 Java 进程继承当前环境。
    $env:DB_USERNAME = $DatabaseUser
    $env:DB_PASSWORD = (New-Object System.Net.NetworkCredential('', $secret)).Password
    $env:DB_URL = $DatabaseUrl
    $env:SERVER_PORT = [string]$Port
    # 切换到 backend 后运行；finally 成对恢复目录，避免影响用户终端后续操作。
    Push-Location $backendDirectory
    try {
        # 调用已安装的 Java；file.encoding 控制进程默认文本编码，-jar 启动 Boot 可执行包。
        & java '-Dfile.encoding=UTF-8' -jar $jarPath
        # 子进程非零退出码说明启动/运行失败，不能仅凭脚本没有抛错就报告成功。
        if ($LASTEXITCODE -ne 0) { throw 'Backend exited unsuccessfully; check database and port configuration.' }
    } finally {
        Pop-Location
    }
} finally {
    # 无论正常结束、密码错误还是端口冲突，都恢复变量并释放 SecureString。
    foreach ($name in $previousValues.Keys) {
        [Environment]::SetEnvironmentVariable($name, $previousValues[$name], 'Process')
    }
    $secret.Dispose()
}
