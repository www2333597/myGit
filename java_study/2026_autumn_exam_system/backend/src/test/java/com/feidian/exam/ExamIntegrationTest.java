/* 文件导读：JUnit 5 + SpringBootTest + MockMvc 集成测试，加载真实 MVC/Service/MyBatis 链路；test profile 默认 H2，测试前严格保护重置目标。 */
// package 划分命名空间与职责；Spring 的组件扫描范围从启动类所在包开始。
package com.feidian.exam;

// import 引入下面使用的类型/静态方法名，不会在这里创建对象、连接数据库或执行请求。
import com.feidian.exam.dto.request.SubmitAttemptRequest;
import com.feidian.exam.dto.response.*;
import com.feidian.exam.security.SessionUser;
import com.feidian.exam.service.ExamService;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// 加载完整 Spring 容器，用 MockMvc 走真实 Controller/Interceptor/Service/Mapper 链路。
@SpringBootTest
@AutoConfigureMockMvc
// 未覆盖 TEST_DB_* 时使用内存 H2；本轮运行还显式指定 H2，禁止把测试变量指向业务库。
@ActiveProfiles("test")
class ExamIntegrationTest {
    // Spring 注入 MockMvc：模拟 Servlet/MVC 请求，不开启真实网络端口。
    @Autowired MockMvc mvc;
    // ObjectMapper：将测试请求对象变 JSON，并将响应 JSON 解析成可逐字段断言的树。
    @Autowired ObjectMapper json;
    // JdbcTemplate：在隔离测试库准备数据、执行只读断言和受控故障注入。
    @Autowired JdbcTemplate jdbc;
    // 注入的是容器管理的考试 Service；并发测试调用它时仍经过 Spring 事务代理。
    @Autowired ExamService exams;
    // 外部允许标志只是保护条件之一，还要核对实际连接主机与专用库名。
    @Value("${test.allow-database-reset:false}") boolean allowDatabaseReset;
    // 根据实际数据库产品设置，用于 MySQL 与 H2 的约束语法差异和证据标签。
    private boolean mysql;

    @BeforeEach
    /** 每个用例前获取测试连接并校验实际库名/产品；仅允许内存 H2 或明确授权的本机 exam_system_test，然后按外键顺序清测试数据并重建虚构种子。 */
    void resetDedicatedTestDatabase() throws Exception {
        try (Connection connection = Objects.requireNonNull(jdbc.getDataSource()).getConnection()) {
            String url = connection.getMetaData().getURL();
            mysql = "MySQL".equals(connection.getMetaData().getDatabaseProductName());
            if (mysql) {
                // 只有显式允许、本机且库名严格为 exam_system_test 时才允许清测试数据。
                // 因此 exam_system_fullstack 永远不会通过这项保护检查。
                URI uri = URI.create(url.substring("jdbc:".length()));
                if (!allowDatabaseReset || !"/exam_system_test".equals(uri.getPath())
                        || !Arrays.asList("127.0.0.1", "localhost").contains(uri.getHost())) {
                    throw new IllegalStateException("Refusing to reset anything except an explicitly allowed local exam_system_test database");
                }
            } else if (!url.startsWith("jdbc:h2:mem:exam_tests")) {
                throw new IllegalStateException("Unexpected test database");
            }
            try (ResultSet tables = connection.getMetaData().getTables(connection.getCatalog(), null, "sys_user", null)) {
                if (!tables.next()) {
                    ScriptUtils.executeSqlScript(connection, new EncodedResource(new ClassPathResource("database/01_schema.sql"), "UTF-8"));
                }
            }
            for (String table : Arrays.asList("exam_attempt_item", "exam_attempt", "question", "enrollment", "course", "sys_user")) {
                jdbc.update("DELETE FROM " + table); // 固定测试表名，不接收外部输入。
            }
            ScriptUtils.executeSqlScript(connection, new EncodedResource(new ClassPathResource("database/02_demo_data.sql"), "UTF-8"));
        }
    }

    /** 用例目的：验证公开健康接口返回 UP，而未登录查本人资料返回 401/UNAUTHENTICATED。 */
    @Test void healthIsPublicAndProfilesAreProtected() throws Exception {
        mvc.perform(get("/api/health")).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("UP"));
        mvc.perform(get("/api/me")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    /** 用例目的：遍历五个虚构账号，验证角色、学工号和关联课程正确，并逐字段确认响应没有 password/passwordHash。 */
    @Test void allDemoAccountsCanLoginAndProfilesHidePasswordHash() throws Exception {
        for (String username : Arrays.asList("teacher01", "teacher02", "student01", "student02", "student03")) {
            Client client = login(username);
            JsonNode profile = getJson(client, "/api/me", 200).get("data");
            assertThat(profile.has("passwordHash")).isFalse();
            assertThat(profile.has("password")).isFalse();
            assertThat(profile.get("role").asText()).isEqualTo(username.startsWith("teacher") ? "TEACHER" : "STUDENT");
            assertThat(profile.get("identityNo").asText()).isNotBlank();
            assertThat(profile.get("courses").size()).isPositive();
        }
    }

    /** 用例目的：先获取匿名会话，再用错误密码登录；不仅验证 401，还验证随后仍不能读取本人资料。 */
    @Test void wrongPasswordDoesNotAuthenticate() throws Exception {
        Client anonymous = anonymous();
        writeJson(anonymous, "POST", "/api/auth/login", map("username", "student01", "password", "wrong-password"), 401);
        getJson(anonymous, "/api/me", 401);
    }

    /** 用例目的：用不存在的账号登录，验证统一 LOGIN_FAILED，避免错误文案直接区分账号不存在与密码错误。 */
    @Test void unknownUserUsesSameFailureResponse() throws Exception {
        Client anonymous = anonymous();
        JsonNode response = writeJson(anonymous, "POST", "/api/auth/login", map("username", "unknown_user", "password", "wrong-password"), 401);
        assertThat(response.get("code").asText()).isEqualTo("LOGIN_FAILED");
    }

    /** 用例目的：验证无 Token 登录失败；正确登录后 Session ID/Token 都变化，旧 Token 无法退出，新 Token 可以退出。 */
    @Test void loginRequiresCsrfAndRotatesSessionIdAndToken() throws Exception {
        mvc.perform(post("/api/auth/login").contentType("application/json")
                .content("{\"username\":\"student01\",\"password\":\"Exam@2026!\"}"))
                .andExpect(status().isForbidden());
        Client client = anonymous();
        String oldId = client.session.getId();
        String oldToken = client.token;
        MvcResult result = write(client, "POST", "/api/auth/login", map("username", "student01", "password", "Exam@2026!"), 200);
        assertThat(result.getRequest().getSession().getId()).isNotEqualTo(oldId);
        String newToken = tree(result).path("data").path("csrfToken").asText();
        assertThat(newToken).isNotEqualTo(oldToken);
        writeJson(client, "POST", "/api/auth/logout", null, 403);
        client.token = newToken;
        writeJson(client, "POST", "/api/auth/logout", null, 200);
    }

    /** 用例目的：已有学生登录会话但写请求缺少 CSRF 头，仍必须拒绝，证明登录检查不能替代 CSRF。 */
    @Test void authenticatedWriteRejectsMissingCsrf() throws Exception {
        Client student = login("student01");
        mvc.perform(post("/api/student/courses/1/attempts").session(student.session)
                .contentType("application/json").content("{\"mode\":\"FIRST\"}"))
                .andExpect(status().isForbidden());
    }

    /** 用例目的：退出后检查 MockHttpSession 已失效，再用无会话请求查资料应返回 401。 */
    @Test void logoutInvalidatesAuthentication() throws Exception {
        Client student = login("student01");
        writeJson(student, "POST", "/api/auth/logout", null, 200);
        assertThat(student.session.isInvalid()).isTrue();
        mvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
    }

    /** 用例目的：先验证去空格后的姓名写入数据库，再尝试注入 role 字段并确认角色仍是 STUDENT。 */
    @Test void profileChangesPersistAndImmutableFieldsAreRejected() throws Exception {
        Client student = login("student01");
        Map<String, Object> update = map("realName", "  新名字  ", "gender", "FEMALE", "phone", "13800008888", "college", "新学院");
        writeJson(student, "PUT", "/api/me", update, 200);
        assertThat(getJson(student, "/api/me", 200).path("data").path("realName").asText()).isEqualTo("新名字");
        assertThat(jdbc.queryForObject("SELECT real_name FROM sys_user WHERE id=1001", String.class)).isEqualTo("新名字");
        update.put("role", "TEACHER");
        writeJson(student, "PUT", "/api/me", update, 400);
        assertThat(jdbc.queryForObject("SELECT role FROM sys_user WHERE id=1001", String.class)).isEqualTo("STUDENT");
    }

    /** 用例目的：构造空白姓名、非法性别、非法电话和空学院，验证 DTO Bean Validation 拦截请求。 */
    @Test void invalidProfileIsRejected() throws Exception {
        Client student = login("student01");
        writeJson(student, "PUT", "/api/me", map("realName", " ", "gender", "OTHER", "phone", "abc", "college", ""), 400);
    }

    /** 用例目的：学生请求教师课程得到 403，并把这次真实 MockMvc 响应保存为失败案例。 */
    @Test void studentCannotAccessTeacherEndpoints() throws Exception {
        Client student = login("student01");
        MvcResult result = mvc.perform(get("/api/teacher/courses").session(student.session))
                .andExpect(status().isForbidden()).andReturn();
        evidence("failure-forbidden", "GET", "/api/teacher/courses", null, result);
    }

    /** 用例目的：教师访问其他教师的课程、题目详情、修改、删除、成绩都得到 403，验证资源级权限。 */
    @Test void teacherCannotAccessAnotherTeachersCourseOrQuestion() throws Exception {
        Client teacher = login("teacher01");
        getJson(teacher, "/api/teacher/courses/2/questions", 403);
        getJson(teacher, "/api/teacher/questions/3", 403);
        writeJson(teacher, "PUT", "/api/teacher/questions/3", question("A", 10), 403);
        writeJson(teacher, "DELETE", "/api/teacher/questions/3", null, 403);
        getJson(teacher, "/api/teacher/courses/2/grades", 403);
    }

    /** 用例目的：教师即使已经登录，也不能调用学生开考接口，验证路径角色隔离。 */
    @Test void teacherCannotStartStudentExam() throws Exception {
        writeJson(login("teacher01"), "POST", "/api/student/courses/1/attempts", map("mode", "FIRST"), 403);
    }

    /** 用例目的：创建题、读取答案、修改题、分页计数、逻辑删除再查询；最后直接查数据库确认 deleted=true。 */
    @Test void questionCrudAndPaginationWork() throws Exception {
        Client teacher = login("teacher01");
        JsonNode created = writeJson(teacher, "POST", "/api/teacher/courses/1/questions", question("C", 15), 201).get("data");
        long id = created.path("id").asLong();
        assertThat(getJson(teacher, "/api/teacher/questions/" + id, 200).path("data").path("correctOption").asText()).isEqualTo("C");
        writeJson(teacher, "PUT", "/api/teacher/questions/" + id, question("D", 25), 200);
        assertThat(getJson(teacher, "/api/teacher/courses/1/questions?page=1&pageSize=1", 200).path("data").path("total").asInt()).isEqualTo(3);
        writeJson(teacher, "DELETE", "/api/teacher/questions/" + id, null, 200);
        getJson(teacher, "/api/teacher/questions/" + id, 404);
        assertThat(jdbc.queryForObject("SELECT deleted FROM question WHERE id=?", Boolean.class, id)).isTrue();
    }

    /** 用例目的：非法选项/分值与越界页码都返回 400；包含 int 最大值，用于验证分页溢出防护。 */
    @Test void invalidQuestionAndWrongPaginationAreRejected() throws Exception {
        Client teacher = login("teacher01");
        writeJson(teacher, "POST", "/api/teacher/courses/1/questions", question("E", 0), 400);
        getJson(teacher, "/api/teacher/courses/1/questions?page=0", 400);
        getJson(teacher, "/api/teacher/courses/1/questions?pageSize=51", 400);
        getJson(teacher, "/api/teacher/courses/1/questions?page=2147483647", 400);
    }

    /** 用例目的：在题目请求里添加 courseId，验证 Jackson 的未知字段拒绝，不能通过 JSON 转移题目归属。 */
    @Test void unknownQuestionFieldsAreRejected() throws Exception {
        Map<String, Object> body = question("A", 10);
        body.put("courseId", 2);
        writeJson(login("teacher01"), "POST", "/api/teacher/courses/1/questions", body, 400);
    }

    /** 用例目的：未选课返回 403，已选但空题库返回 409/NO_QUESTIONS；最后确认没有任何空答卷被创建。 */
    @Test void unselectedCourseAndEmptyCourseCannotStart() throws Exception {
        Client student = login("student01");
        writeJson(student, "POST", "/api/student/courses/2/attempts", map("mode", "FIRST"), 403);
        JsonNode empty = writeJson(student, "POST", "/api/student/courses/3/attempts", map("mode", "FIRST"), 409);
        assertThat(empty.path("code").asText()).isEqualTo("NO_QUESTIONS");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM exam_attempt", Integer.class)).isZero();
    }

    /** 用例目的：从未参加考试却申请 RETAKE 返回 409；未知模式在请求校验阶段返回 400。 */
    @Test void retakeWithoutHistoryAndUnknownModeAreRejected() throws Exception {
        Client student = login("student01");
        writeJson(student, "POST", "/api/student/courses/1/attempts", map("mode", "RETAKE"), 409);
        writeJson(student, "POST", "/api/student/courses/1/attempts", map("mode", "SOMETHING"), 400);
    }

    /** 用例目的：重复 FIRST 返回同一答卷；学生响应无标准答案/密码/已选项，交卷前查成绩为 409，概览状态为进行中。 */
    @Test void startingAgainReusesAttemptAndNeverLeaksCorrectAnswers() throws Exception {
        Client student = login("student01");
        long id = start(student, "FIRST");
        JsonNode again = writeJson(student, "POST", "/api/student/courses/1/attempts", map("mode", "FIRST"), 200);
        assertThat(again.path("data").path("attemptId").asLong()).isEqualTo(id);
        JsonNode detail = getJson(student, "/api/student/attempts/" + id, 200).get("data");
        assertThat(detail.path("items").size()).isEqualTo(2);
        assertThat(detail.toString()).doesNotContain("correctOption", "passwordHash", "chosenOption");
        getJson(student, "/api/student/attempts/" + id + "/result", 409);
        assertThat(getJson(student, "/api/student/courses", 200).path("data").get(0).path("examStatus").asText()).isEqualTo("IN_PROGRESS");
    }

    /** 用例目的：完整首次考试得 30 分，重复交卷不改成绩时间；重考产生新卷并保留旧卷；教师看到最近 0 分，姓名注入文本不能改变 SQL 结构。 */
    @Test void completeWorkflowGradesOnServerAndPreservesRetakeHistory() throws Exception {
        Client student = login("student01");
        long first = start(student, "FIRST");
        Map<String, Object> answers = correctAnswers(student, first);
        MvcResult submitted = write(student, "POST", "/api/student/attempts/" + first + "/submit", answers, 200);
        evidence("success-submit", "POST", "/api/student/attempts/" + first + "/submit", answers, submitted);
        assertThat(tree(submitted).path("data").path("score").asInt()).isEqualTo(30);
        assertThat(tree(submitted).path("data").path("submittedAt").asText()).matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}");
        assertThat(getJson(student, "/api/student/courses", 200).path("data").get(0).path("examStatus").asText()).isEqualTo("COMPLETED");
        writeJson(student, "POST", "/api/student/courses/1/attempts", map("mode", "FIRST"), 409);
        JsonNode duplicate = writeJson(student, "POST", "/api/student/attempts/" + first + "/submit", map("answers", Collections.emptyList()), 200);
        assertThat(duplicate.get("data")).isEqualTo(tree(submitted).get("data"));
        long second = start(student, "RETAKE");
        assertThat(second).isNotEqualTo(first);
        writeJson(student, "POST", "/api/student/attempts/" + second + "/submit", map("answers", Collections.emptyList()), 200);
        assertThat(getJson(student, "/api/student/courses/1/attempts", 200).path("data").path("total").asInt()).isEqualTo(2);
        assertThat(getJson(student, "/api/student/attempts/" + first + "/result", 200).path("data").path("score").asInt()).isEqualTo(30);
        Client teacher = login("teacher01");
        JsonNode latest = getJson(teacher, "/api/teacher/courses/1/grades?studentName=演示学生甲", 200).path("data");
        assertThat(latest.path("total").asInt()).isEqualTo(1);
        assertThat(latest.path("items").get(0).path("score").asInt()).isZero();
        assertThat(getJson(teacher, "/api/teacher/courses/1/students/1001/attempts", 200).path("data").path("total").asInt()).isEqualTo(2);
        assertThat(getJson(teacher, "/api/teacher/courses/1/grades?studentName=' OR 1=1 --", 200).path("data").path("total").asInt()).isZero();
    }

    /** 用例目的：另一学生猜中答卷 ID 也无法查看、查分或提交，一律 404。 */
    @Test void otherStudentsCannotReadOrSubmitAttempt() throws Exception {
        Client owner = login("student01");
        long id = start(owner, "FIRST");
        Client other = login("student02");
        getJson(other, "/api/student/attempts/" + id, 404);
        getJson(other, "/api/student/attempts/" + id + "/result", 404);
        writeJson(other, "POST", "/api/student/attempts/" + id + "/submit", map("answers", Collections.emptyList()), 404);
    }

    /** 用例目的：逐个验证重复 ID、外来 ID、null 元素、非法选项、伪造 score；所有失败后答卷仍在进行中且明细没有部分得分。 */
    @Test void malformedOrTamperedAnswersDoNotChangeAttempt() throws Exception {
        Client student = login("student01");
        long id = start(student, "FIRST");
        long itemId = getJson(student, "/api/student/attempts/" + id, 200).path("data").path("items").get(0).path("itemId").asLong();
        List<Object> duplicate = Arrays.asList(map("itemId", itemId, "selectedOption", "A"), map("itemId", itemId, "selectedOption", "B"));
        String path = "/api/student/attempts/" + id + "/submit";
        writeJson(student, "POST", path, map("answers", duplicate), 400);
        writeJson(student, "POST", path, map("answers", Collections.singletonList(map("itemId", Long.MAX_VALUE, "selectedOption", "A"))), 400);
        writeJson(student, "POST", path, map("answers", Arrays.asList((Object) null)), 400);
        writeJson(student, "POST", path, map("answers", Collections.singletonList(map("itemId", itemId, "selectedOption", "E"))), 400);
        writeJson(student, "POST", path, map("answers", Collections.emptyList(), "score", 100), 400);
        assertThat(jdbc.queryForObject("SELECT status FROM exam_attempt WHERE id=?", String.class, id)).isEqualTo("IN_PROGRESS");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM exam_attempt_item WHERE earned_points IS NOT NULL", Integer.class)).isZero();
    }

    /** 用例目的：坏 JSON 返回 400，不存在答卷返回 404，响应不泄露堆栈/SQL 异常；保存一个异常案例。 */
    @Test void invalidJsonAndMissingResourcesHaveStableErrors() throws Exception {
        Client student = login("student01");
        MvcResult result = mvc.perform(post("/api/student/courses/1/attempts").session(student.session)
                .header("X-CSRF-Token", student.token).contentType("application/json").content("{broken"))
                .andExpect(status().isBadRequest()).andReturn();
        evidence("exception-invalid-json", "POST", "/api/student/courses/1/attempts", "{broken", result);
        getJson(student, "/api/student/attempts/99999999", 404);
        assertThat(tree(result).toString()).doesNotContain("stackTrace", "SQLException");
    }

    /** 用例目的：开考后改第 1 题答案/分值、删第 2 题；旧卷仍得原 30 分，新重考只含修改后 99 分题，旧成绩保持不变。 */
    @Test void snapshotsSurviveQuestionEditAndLogicalDelete() throws Exception {
        Client student = login("student01");
        long oldId = start(student, "FIRST");
        Map<String, Object> originalAnswers = correctAnswers(student, oldId);
        Client teacher = login("teacher01");
        writeJson(teacher, "PUT", "/api/teacher/questions/1", question("B", 99), 200);
        writeJson(teacher, "DELETE", "/api/teacher/questions/2", null, 200);
        JsonNode original = writeJson(student, "POST", "/api/student/attempts/" + oldId + "/submit", originalAnswers, 200);
        assertThat(original.path("data").path("score").asInt()).isEqualTo(30);
        long next = start(student, "RETAKE");
        JsonNode changed = getJson(student, "/api/student/attempts/" + next, 200).path("data");
        assertThat(changed.path("items").size()).isEqualTo(1);
        assertThat(changed.path("totalScore").asInt()).isEqualTo(99);
        long newItem = changed.path("items").get(0).path("itemId").asLong();
        JsonNode result = writeJson(student, "POST", "/api/student/attempts/" + next + "/submit",
                map("answers", Collections.singletonList(map("itemId", newItem, "selectedOption", "B"))), 200);
        assertThat(result.path("data").path("score").asInt()).isEqualTo(99);
        assertThat(getJson(student, "/api/student/attempts/" + oldId + "/result", 200).path("data").path("score").asInt()).isEqualTo(30);
    }

    /** 用例目的：四个线程同时调用被 Spring 代理的开考 Service；所有返回同一 ID，数据库只有一张卷和两条题目明细。 */
    @Test void concurrentStartCreatesOnlyOneAttempt() throws Exception {
        SessionUser student = new SessionUser(1001L, "STUDENT");
        List<StartAttemptResponse> results = concurrently(() -> exams.start(student, 1L, "FIRST"));
        long id = results.get(0).attemptId;
        assertThat(results).allSatisfy(value -> assertThat(value.attemptId).isEqualTo(id));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM exam_attempt", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM exam_attempt_item", Integer.class)).isEqualTo(2);
    }

    /** 用例目的：四线程提交同一答卷；返回同样分数和时间，数据库明细得分之和只有一份结果。 */
    @Test void concurrentSubmitSavesOneStableResult() throws Exception {
        Client client = login("student01");
        long id = start(client, "FIRST");
        SubmitAttemptRequest body = json.convertValue(correctAnswers(client, id), SubmitAttemptRequest.class);
        List<ResultResponse> results = concurrently(() -> exams.submit(new SessionUser(1001L, "STUDENT"), id, body));
        assertThat(results).allSatisfy(value -> {
            assertThat(value.score).isEqualTo(30);
            assertThat(value.submittedAt).isEqualTo(results.get(0).submittedAt);
        });
        assertThat(jdbc.queryForObject("SELECT SUM(earned_points) FROM exam_attempt_item WHERE attempt_id=?", Integer.class, id)).isEqualTo(30);
    }

    /** 用例目的：仅在隔离测试库用临时 CHECK 让第 2 题保存失败，验证第 1 题/总分/状态全回滚；finally 移除约束后重试成功。 */
    @Test void failureHalfwayThroughSubmissionRollsEverythingBackAndAllowsRetry() throws Exception {
        Client student = login("student01");
        long id = start(student, "FIRST");
        Map<String, Object> body = correctAnswers(student, id);
        // 只在专用测试库增加临时约束，使第二道题写入失败，真实检验事务回滚。
        jdbc.execute("ALTER TABLE exam_attempt_item ADD CONSTRAINT ck_test_fail CHECK (earned_points IS NULL OR position_no <> 2)");
        try {
            MvcResult failure = write(student, "POST", "/api/student/attempts/" + id + "/submit", body, 500);
            evidence("exception-rollback", "POST", "/api/student/attempts/" + id + "/submit", body, failure);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM exam_attempt_item WHERE earned_points IS NOT NULL", Integer.class)).isZero();
            assertThat(jdbc.queryForObject("SELECT status FROM exam_attempt WHERE id=?", String.class, id)).isEqualTo("IN_PROGRESS");
            assertThat(jdbc.queryForObject("SELECT score FROM exam_attempt WHERE id=?", Integer.class, id)).isNull();
        } finally {
            jdbc.execute("ALTER TABLE exam_attempt_item DROP " + (mysql ? "CHECK" : "CONSTRAINT") + " ck_test_fail");
        }
        assertThat(writeJson(student, "POST", "/api/student/attempts/" + id + "/submit", body, 200).path("data").path("score").asInt()).isEqualTo(30);
    }

    /** 用例目的：模拟 OPTIONS 预检，允许名单来源取得凭据响应头，陌生来源返回 403 且无允许源头。 */
    @Test void corsOnlyAllowsConfiguredFrontendOrigins() throws Exception {
        mvc.perform(options("/api/me").header("Origin", "http://localhost:5173").header("Access-Control-Request-Method", "PUT")
                .header("Access-Control-Request-Headers", "X-CSRF-Token,Content-Type"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Credentials", "true"));
        mvc.perform(options("/api/me").header("Origin", "https://untrusted.example").header("Access-Control-Request-Method", "PUT"))
                .andExpect(status().isForbidden()).andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    /** 用例目的：MockMvc 模拟两个文档域名/端口，按 OpenAPI servers 解析健康检查地址，验证未被写死到 8080；没有真的启动这些端口。 */
    @Test void openApiUsesTheRequestedHostAndPort() throws Exception {
        // 只用 MockMvc 模拟请求地址，不实际开放端口或访问局域网。
        for (String address : Arrays.asList("http://127.0.0.1:9097/v3/api-docs",
                "http://localhost:8082/v3/api-docs")) {
            URI documentUrl = URI.create(address);
            MvcResult result = mvc.perform(get(address)).andExpect(status().isOk()).andReturn();
            String serverUrl = tree(result).path("servers").get(0).path("url").asText();
            URI apiUrl = documentUrl.resolve(serverUrl).resolve("api/health");
            assertThat(apiUrl.getHost()).isEqualTo(documentUrl.getHost());
            assertThat(apiUrl.getPort()).isEqualTo(documentUrl.getPort());
            assertThat(apiUrl.getPath()).isEqualTo("/api/health");
        }
    }

    /** 用例目的：检查自动生成文档包含真实路径、Cookie/CSRF 同时要求和创建 201，再输出可导入的 OpenAPI JSON。 */
    @Test void openApiExportContainsActualEndpoints() throws Exception {
        MvcResult result = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn();
        JsonNode document = tree(result);
        assertThat(document.path("paths").has("/api/student/attempts/{attemptId}/submit")).isTrue();
        assertThat(document.path("paths").has("/api/teacher/courses/{courseId}/grades")).isTrue();
        JsonNode start = document.path("paths").path("/api/student/courses/{courseId}/attempts").path("post");
        assertThat(start.path("security").get(0).has("sessionCookie")).isTrue();
        assertThat(start.path("security").get(0).has("csrfToken")).isTrue();
        assertThat(start.path("responses").has("201")).isTrue();
        Path output = Paths.get("target", "generated-api", "openapi.json");
        Files.createDirectories(output.getParent());
        Files.write(output, json.writerWithDefaultPrettyPrinter().writeValueAsBytes(document));
    }

    /** 用 GET csrf 建立匿名 Mock Session 并保存 Token，模拟浏览器拿 Cookie 后再登录的第一步。 */
    private Client anonymous() throws Exception {
        MvcResult result = mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andReturn();
        return new Client((MockHttpSession) result.getRequest().getSession(false), tree(result).path("data").path("csrfToken").asText());
    }

    /** 先建匿名会话，再提交公开演示凭据；读取认证后的 Session 和新 Token，避免后续请求误用旧 Token。 */
    private Client login(String username) throws Exception {
        Client client = anonymous();
        MvcResult result = write(client, "POST", "/api/auth/login", map("username", username, "password", "Exam@2026!"), 200);
        client.session = (MockHttpSession) result.getRequest().getSession(false);
        client.token = tree(result).path("data").path("csrfToken").asText();
        return client;
    }

    /** 辅助方法仅为演示课程 1 创建新答卷，要求 201 后提取实际 attemptId，避免硬编码自增编号。 */
    private long start(Client client, String mode) throws Exception {
        return writeJson(client, "POST", "/api/student/courses/1/attempts", map("mode", mode), 201).path("data").path("attemptId").asLong();
    }

    /** 用指定模拟会话 GET 接口，断言预期状态，再解析响应 JSON。 */
    private JsonNode getJson(Client client, String path, int expected) throws Exception {
        return tree(mvc.perform(get(path).session(client.session)).andExpect(status().is(expected)).andReturn());
    }

    /** 调用 write 统一发送有会话/Token 的写请求，再把结果解析为 JSON。 */
    private JsonNode writeJson(Client client, String method, String path, Object body, int expected) throws Exception {
        return tree(write(client, method, path, body, expected));
    }

    /** 按 POST/PUT/DELETE 构造请求，携带 Session、Token 和 UTF-8 JSON，再断言状态码；与业务 Controller 使用同一条 MVC 链。 */
    private MvcResult write(Client client, String method, String path, Object body, int expected) throws Exception {
        MockHttpServletRequestBuilder request = "POST".equals(method) ? post(path) : "PUT".equals(method) ? put(path) : delete(path);
        request.session(client.session).header("X-CSRF-Token", client.token).contentType("application/json").characterEncoding("UTF-8");
        if (body != null) request.content(json.writeValueAsString(body));
        return mvc.perform(request).andExpect(status().is(expected)).andReturn();
    }

    /** 将 MockMvc 响应按 UTF-8 解析成 JsonNode，后续用 path/get 取字段进行断言。 */
    private JsonNode tree(MvcResult result) throws Exception {
        return json.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    /** 只针对固定测试种子：第一题 A、第二题 B；实际 itemId 从本次试卷取得，这不是向真实学生提供标准答案的接口。 */
    private Map<String, Object> correctAnswers(Client client, long attemptId) throws Exception {
        JsonNode items = getJson(client, "/api/student/attempts/" + attemptId, 200).path("data").path("items");
        List<Object> answers = new ArrayList<>();
        for (JsonNode item : items) {
            String option = item.path("positionNo").asInt() == 1 ? "A" : "B";
            answers.add(map("itemId", item.path("itemId").asLong(), "selectedOption", option));
        }
        return map("answers", answers);
    }

    /** 构造测试用的完整题目输入 Map，参数用于变化标准答案和分值，其他文本保持固定。 */
    private Map<String, Object> question(String answer, int points) {
        return map("stem", "演示题目", "optionA", "选项甲", "optionB", "选项乙", "optionC", "选项丙",
                "optionD", "选项丁", "correctOption", answer, "points", points);
    }

    /** 用交替的 key/value 可变参数创建 LinkedHashMap，只供受控测试数据简写；不是可接收任意外部输入的通用校验器。 */
    private Map<String, Object> map(Object... values) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) result.put((String) values[i], values[i + 1]);
        return result;
    }

    /** 固定 4 线程在 CountDownLatch 放行后同时执行任务；Future 限时取结果，finally 关闭线程池，验证真实数据库事务竞争。 */
    private <T> List<T> concurrently(Callable<T> task) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(4);
        // 闩锁初值为 1，让线程先等待，再由主测试线程统一放行，增加并发重叠机会。
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<T>> futures = new ArrayList<>();
            for (int i = 0; i < 4; i++) futures.add(pool.submit(() -> {
                if (!start.await(10, TimeUnit.SECONDS)) throw new TimeoutException();
                return task.call();
            }));
            start.countDown();
            List<T> result = new ArrayList<>();
            // get 会等待任务完成并传播异常；设置超时避免锁错误让测试无限挂住。
            for (Future<T> future : futures) result.add(future.get(20, TimeUnit.SECONDS));
            return result;
        } finally {
            // 无论成功/失败都收回工作线程，避免影响后续测试或 Maven 进程退出。
            pool.shutdownNow();
            pool.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    /** 只输出请求体、业务响应、状态、时间和测试环境到 target/generated-api/examples；不写 Cookie、CSRF 头或 MySQL 密码。 */
    private void evidence(String name, String method, String path, Object body, MvcResult result) throws Exception {
        ObjectNode document = json.createObjectNode();
        document.put("transport", "Spring MockMvc");
        document.put("database", mysql ? "MySQL" : "H2 (MySQL compatibility mode)");
        document.put("verifiedAt", OffsetDateTime.now().toString());
        document.put("method", method);
        document.put("path", path);
        document.set("requestBody", json.valueToTree(body));
        document.put("status", result.getResponse().getStatus());
        document.set("responseBody", tree(result));
        Path output = Paths.get("target", "generated-api", "examples", name + ".json");
        Files.createDirectories(output.getParent());
        Files.write(output, json.writerWithDefaultPrettyPrinter().writeValueAsBytes(document));
    }

    /** 仅测试使用的会话载体；模拟 Cookie 指向的 Session 与当前 CSRF Token，和业务 User 不同。 */
    private static class Client {
        MockHttpSession session;
        String token;
        /** 保存模拟会话与 Token；登录时调用方会更新这两个值。 */
        Client(MockHttpSession session, String token) { this.session = session; this.token = token; }
    }
}
