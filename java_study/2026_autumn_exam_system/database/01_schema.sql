-- 文件导读：本文件是建表 DDL，只供空库初始化/隔离测试读取；本轮仅补注释，不执行 SQL。
-- InnoDB 提供事务与行锁；utf8mb4 支持完整 Unicode。CHECK 的有效执行要求 MySQL 8.0.16+。
-- 仅在专门创建的空数据库 exam_system_fullstack 中手动执行。此文件不删除表或现有数据。
-- 账号表：教师/学生共用一张表，用角色区分；账号密码存 BCrypt 哈希。主键、唯一约束和 CHECK 分别保护标识、重复与合法取值。
CREATE TABLE sys_user (
  -- sys_user 的自增主键；业务插入后由 JDBC generated keys 回填，插入方法返回值本身是影响行数。
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  -- 登录账号（3–32 位字母、数字、下划线）。
  username VARCHAR(32) NOT NULL,
  -- BCrypt 密码哈希，不能当明文解密或返回客户端。
  password_hash VARCHAR(100) NOT NULL,
  -- 角色 TEACHER/STUDENT，真正授权依据来自服务端。
  role VARCHAR(16) NOT NULL,
  -- 真实姓名/演示姓名。
  real_name VARCHAR(50) NOT NULL,
  -- 性别枚举 UNKNOWN/MALE/FEMALE。
  gender VARCHAR(8) NOT NULL DEFAULT 'UNKNOWN',
  -- 电话号码文本，保留前导零和加号。
  phone VARCHAR(20) NOT NULL DEFAULT '',
  -- 学生学号或教师工号（不是身份证号）。
  identity_no VARCHAR(32) NOT NULL,
  -- 所属学院。
  college VARCHAR(100) NOT NULL,
  -- 登录名唯一，避免一次登录查到多个用户；唯一约束同时产生唯一索引。
  CONSTRAINT uk_user_username UNIQUE (username),
  -- 同角色内学工号唯一；不同角色可使用相同 identity_no。
  CONSTRAINT uk_user_identity UNIQUE (role, identity_no),
  -- 限制角色为教师/学生，业务层仍需控制谁可以以该角色操作。
  CONSTRAINT ck_user_role CHECK (role IN ('TEACHER','STUDENT')),
  -- 限制允许的性别字面值；应用 DTO 的正则给出更早的输入反馈。
  CONSTRAINT ck_user_gender CHECK (gender IN ('UNKNOWN','MALE','FEMALE'))
-- 选择 InnoDB 存储引擎和 utf8mb4 字符集；不指定 ON DELETE CASCADE，避免自动级联清理历史记录。
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 课程表：每门课程关联一个教师账号。外键只保证账号存在，教师角色由业务规则和初始化数据保证。
CREATE TABLE course (
  -- course 的自增主键；业务插入后由 JDBC generated keys 回填，插入方法返回值本身是影响行数。
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  -- 课程名称。
  name VARCHAR(100) NOT NULL,
  -- 授课教师 sys_user.id。
  teacher_id BIGINT NOT NULL,
  -- 课程说明。
  description VARCHAR(500) NOT NULL DEFAULT '',
  -- 课程教师 ID 引用用户表；外键本身不检查该用户 role 是否为 TEACHER。
  CONSTRAINT fk_course_teacher FOREIGN KEY (teacher_id) REFERENCES sys_user(id),
  -- 支撑按教师 ID 列出课程；索引列应与 WHERE/JOIN 条件一致。
  INDEX idx_course_teacher (teacher_id)
-- 选择 InnoDB 存储引擎和 utf8mb4 字符集；不指定 ON DELETE CASCADE，避免自动级联清理历史记录。
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 选课中间表：学生与课程多对多。唯一的学生/课程组合同时为并发开考提供稳定锁定记录。
CREATE TABLE enrollment (
  -- enrollment 的自增主键；业务插入后由 JDBC generated keys 回填，插入方法返回值本身是影响行数。
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  -- 学生 sys_user.id。
  student_id BIGINT NOT NULL,
  -- 所属课程 course.id。
  course_id BIGINT NOT NULL,
  -- 唯一约束保证同一学生不能重复选择同一课程，也是并发开考锁定的稳定记录。
  -- 防止同一学生重复选同一门课，并为精确查选课提供联合索引。
  CONSTRAINT uk_enrollment UNIQUE (student_id, course_id),
  -- 选课的学生账号必须存在；STUDENT 角色需由业务保证。
  CONSTRAINT fk_enrollment_student FOREIGN KEY (student_id) REFERENCES sys_user(id),
  -- 选课引用的课程必须存在，防止悬空课程 ID。
  CONSTRAINT fk_enrollment_course FOREIGN KEY (course_id) REFERENCES course(id)
-- 选择 InnoDB 存储引擎和 utf8mb4 字符集；不指定 ON DELETE CASCADE，避免自动级联清理历史记录。
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 当前题库：新开考试读取未删除题目，修改不会反向覆盖已生成的快照。时间由 MySQL 默认值/更新 SQL 管理。
CREATE TABLE question (
  -- question 的自增主键；业务插入后由 JDBC generated keys 回填，插入方法返回值本身是影响行数。
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  -- 所属课程 course.id。
  course_id BIGINT NOT NULL,
  -- 题干文本。
  stem VARCHAR(2000) NOT NULL,
  -- A 选项文本。
  option_a VARCHAR(500) NOT NULL,
  -- B 选项文本。
  option_b VARCHAR(500) NOT NULL,
  -- C 选项文本。
  option_c VARCHAR(500) NOT NULL,
  -- D 选项文本。
  option_d VARCHAR(500) NOT NULL,
  -- 标准答案字母 A/B/C/D。
  correct_option CHAR(1) NOT NULL,
  -- 单题正整数分值（1–100）。
  points INT NOT NULL,
  -- 逻辑删除标志，true 时不参加新考试。
  deleted BOOLEAN NOT NULL DEFAULT FALSE,
  -- 题库记录创建时间，默认 CURRENT_TIMESTAMP 由 MySQL 产生。
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  -- 题库最近更新时间，由题目更新/删除 SQL 显式刷新。
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  -- 题库题目必须属于已存在课程。
  CONSTRAINT fk_question_course FOREIGN KEY (course_id) REFERENCES course(id),
  -- 标准答案只能取 A/B/C/D，不存具体选项全文作为判分键。
  CONSTRAINT ck_question_option CHECK (correct_option IN ('A','B','C','D')),
  -- 每题分值限定为 1–100；与 QuestionRequest/快照约束保持一致。
  CONSTRAINT ck_question_points CHECK (points BETWEEN 1 AND 100),
  -- 联合索引按 course_id、deleted、id 排列，面向课程有效题目筛选与顺序读取。
  INDEX idx_question_course (course_id, deleted, id)
-- 选择 InnoDB 存储引擎和 utf8mb4 字符集；不指定 ON DELETE CASCADE，避免自动级联清理历史记录。
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 答卷头：记录一次考试的所有者、次数、状态、满分和最终成绩；一门课重考需要新记录而不是覆盖旧成绩。
CREATE TABLE exam_attempt (
  -- exam_attempt 的自增主键；业务插入后由 JDBC generated keys 回填，插入方法返回值本身是影响行数。
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  -- 学生 sys_user.id。
  student_id BIGINT NOT NULL,
  -- 所属课程 course.id。
  course_id BIGINT NOT NULL,
  -- 该学生该课程的考试次数序号，从 1 开始。
  attempt_no INT NOT NULL,
  -- 答卷状态 IN_PROGRESS/SUBMITTED。
  status VARCHAR(16) NOT NULL,
  -- 实际得分，未交卷时为 null，0 分表示已经判分但没有得分。
  score INT NULL,
  -- 开考时整张试卷的满分，不固定为 100。
  total_score INT NOT NULL,
  -- 服务端记录的开考时间，本项目按北京时间处理。
  started_at DATETIME NOT NULL,
  -- 第一次成功交卷的时间，重复提交不更新它。
  submitted_at DATETIME NULL,
  -- 应用层行锁负责正常并发流程，唯一约束作为数据库最后一道一致性防线。
  -- 每个学生/课程/次数组合唯一；它不是“只能有一张进行中答卷”的专用约束，正常并发仍靠选课行锁。
  CONSTRAINT uk_attempt_number UNIQUE (student_id, course_id, attempt_no),
  -- 每张答卷必须有已存在学生账号。
  CONSTRAINT fk_attempt_student FOREIGN KEY (student_id) REFERENCES sys_user(id),
  -- 每张答卷必须引用已存在课程。
  CONSTRAINT fk_attempt_course FOREIGN KEY (course_id) REFERENCES course(id),
  -- 限制存储的状态只为 IN_PROGRESS 或 SUBMITTED，课程概览 COMPLETED 不存于此字段。
  CONSTRAINT ck_attempt_status CHECK (status IN ('IN_PROGRESS','SUBMITTED')),
  -- 考试次数从 1 开始，不能为零或负数。
  CONSTRAINT ck_attempt_number CHECK (attempt_no > 0),
  -- 卷面满分必须为正数；空题库还会在 Service 提前拒绝。
  CONSTRAINT ck_attempt_total CHECK (total_score > 0),
  -- 约束状态/分数/时间组合：进行中没有成绩时间；已交卷得分在 0 到满分之间且有提交时间。
  CONSTRAINT ck_attempt_result CHECK (
    (status='IN_PROGRESS' AND score IS NULL AND submitted_at IS NULL)
    OR (status='SUBMITTED' AND score IS NOT NULL AND score BETWEEN 0 AND total_score AND submitted_at IS NOT NULL)),
  -- 支撑按课程及已交卷状态查询并按时间/ID 排序；实际性能还需 EXPLAIN 和真实数据验证。
  INDEX idx_attempt_grade (course_id, status, submitted_at, id)
-- 选择 InnoDB 存储引擎和 utf8mb4 字符集；不指定 ON DELETE CASCADE，避免自动级联清理历史记录。
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 答卷明细：开考时保存题干/选项/标准答案/分值快照，交卷时写最终选择和得分。来源题不物理删除以保持外键。
CREATE TABLE exam_attempt_item (
  -- exam_attempt_item 的自增主键；业务插入后由 JDBC generated keys 回填，插入方法返回值本身是影响行数。
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  -- 本次考试 exam_attempt.id。
  attempt_id BIGINT NOT NULL,
  -- 来源题库 question.id，用于追溯而不是作为交卷 itemId。
  source_question_id BIGINT NOT NULL,
  -- 本次试卷中的题序，从 1 开始。
  position_no INT NOT NULL,
  -- 快照保证教师后来改题或逻辑删题，不会改变已经开出的试卷与判分依据。
  -- 开考时的题干副本。
  stem_snapshot VARCHAR(2000) NOT NULL,
  -- 开考时的 A 选项副本。
  option_a_snapshot VARCHAR(500) NOT NULL,
  -- 开考时的 B 选项副本。
  option_b_snapshot VARCHAR(500) NOT NULL,
  -- 开考时的 C 选项副本。
  option_c_snapshot VARCHAR(500) NOT NULL,
  -- 开考时的 D 选项副本。
  option_d_snapshot VARCHAR(500) NOT NULL,
  -- 开考时的标准答案，只有服务端判分可用。
  correct_option_snapshot CHAR(1) NOT NULL,
  -- 开考时单题满分，教师改题不改变它。
  points_snapshot INT NOT NULL,
  -- 学生最终选择；漏答为 null。
  chosen_option CHAR(1) NULL,
  -- 这道题最终所得分；未交卷为 null、漏答或答错为 0。
  earned_points INT NULL,
  -- 同一张卷不能有两个相同题序。
  CONSTRAINT uk_item_position UNIQUE (attempt_id, position_no),
  -- 同一张卷不能重复加入同一来源题；重考是另一张卷，所以仍可再次使用该题。
  CONSTRAINT uk_item_source UNIQUE (attempt_id, source_question_id),
  -- 明细必须依附于已有答卷头，因此开考先插卷头再插明细。
  CONSTRAINT fk_item_attempt FOREIGN KEY (attempt_id) REFERENCES exam_attempt(id),
  -- 保留与题库来源的外键关系，解释题目为何采用逻辑删除。
  CONSTRAINT fk_item_question FOREIGN KEY (source_question_id) REFERENCES question(id),
  -- 题序从 1 开始。
  CONSTRAINT ck_item_position CHECK (position_no > 0),
  -- 快照标准答案限定 A/B/C/D。
  CONSTRAINT ck_item_correct CHECK (correct_option_snapshot IN ('A','B','C','D')),
  -- 快照单题满分限定 1–100。
  CONSTRAINT ck_item_points CHECK (points_snapshot BETWEEN 1 AND 100),
  -- 学生选项可为 null（漏答）或 A/B/C/D。
  CONSTRAINT ck_item_chosen CHECK (chosen_option IS NULL OR chosen_option IN ('A','B','C','D')),
  -- 得分可为 null（未判分），判分后必须在零与本题满分之间；总分与逐题和仍由事务业务保证。
  CONSTRAINT ck_item_earned CHECK (earned_points IS NULL OR earned_points BETWEEN 0 AND points_snapshot)
-- 选择 InnoDB 存储引擎和 utf8mb4 字符集；不指定 ON DELETE CASCADE，避免自动级联清理历史记录。
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
