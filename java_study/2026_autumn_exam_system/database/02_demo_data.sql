-- 仅用于本地演示，所有姓名、手机号、学号和课程均为虚构数据。
-- 演示账号共用公开演示密码 Exam@2026!；不要将这些账号用于公网环境。
-- 文件导读：这是种子 DML；按用户 -> 课程 -> 选课 -> 题目顺序插入，满足 MySQL 外键依赖。
-- 不是幂等脚本：固定主键/唯一账号已经存在时再次执行会冲突；当前已初始化的库不需要重跑。
-- 这里的哈希对应公开应用演示密码，不是本机 MySQL root 密码。
-- 两名教师负责不同课程，三名学生选课不同，用于验证角色和资源归属隔离。
INSERT INTO sys_user (id,username,password_hash,role,real_name,gender,phone,identity_no,college) VALUES
(1,'teacher01','$2a$10$esUZrff.exrul4AeamUuIuGcTSQSEzwf0tHNZqx5a6FFsUwPBYHf2','TEACHER','演示教师甲','UNKNOWN','13800000001','T001','示例计算机学院'),
(2,'teacher02','$2a$10$esUZrff.exrul4AeamUuIuGcTSQSEzwf0tHNZqx5a6FFsUwPBYHf2','TEACHER','演示教师乙','UNKNOWN','13800000002','T002','示例计算机学院'),
(1001,'student01','$2a$10$esUZrff.exrul4AeamUuIuGcTSQSEzwf0tHNZqx5a6FFsUwPBYHf2','STUDENT','演示学生甲','UNKNOWN','13800001001','S001','示例计算机学院'),
(1002,'student02','$2a$10$esUZrff.exrul4AeamUuIuGcTSQSEzwf0tHNZqx5a6FFsUwPBYHf2','STUDENT','演示学生乙','UNKNOWN','13800001002','S002','示例计算机学院'),
(1003,'student03','$2a$10$esUZrff.exrul4AeamUuIuGcTSQSEzwf0tHNZqx5a6FFsUwPBYHf2','STUDENT','演示学生丙','UNKNOWN','13800001003','S003','示例计算机学院');

-- 课程 3 特意没有题目，验证“已选课程但空题库”应返回 NO_QUESTIONS，而非生成空卷。
INSERT INTO course (id,name,teacher_id,description) VALUES
(1,'Java 基础',1,'单选题演示课程'),
(2,'SQL 基础',2,'权限隔离演示课程'),
(3,'空题库演示',1,'用于验证空题库不能开考');

-- 学生甲选择 Java/空题库，学生乙选择 Java/SQL，学生丙只选 SQL，便于演示越权失败。
INSERT INTO enrollment (id,student_id,course_id) VALUES
(1,1001,1),(2,1001,3),(3,1002,1),(4,1002,2),(5,1003,2);

-- Java 两题 A/10 分、B/20 分，总分 30；SQL 一题 C/15 分，证明满分不应硬编码为 100。
-- 考试及答卷明细不在这里预造，由实际开考/交卷业务生成，便于观察完整持久化流程。
INSERT INTO question (id,course_id,stem,option_a,option_b,option_c,option_d,correct_option,points) VALUES
(1,1,'Java 中哪个关键字用于继承类？','extends','implements','import','package','A',10),
(2,1,'下列哪一种集合通常允许重复元素？','Set','List','仅 TreeSet','以上都不允许','B',20),
(3,2,'关系表中用于唯一标识一条记录的约束是？','DEFAULT','ORDER BY','PRIMARY KEY','GROUP BY','C',15);
