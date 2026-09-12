# 在线考试系统

JDK 8 + Spring Boot + MyBatis + MySQL 的前后端分离项目后端，面向教师和学生。

当前已实现：登录与角色权限、个人资料、课程题库增删改查、在线考试、服务端判分、重考、历史成绩、分页和姓名筛选。包含 20 个接口、六张表、初始化数据和自动化测试。前端页面由前端成员实现，本目录不包含前端应用。

## 目录

```text
backend/        Maven 后端源码与测试
database/       建表脚本和虚构演示数据
api/            OpenAPI 定义、响应案例、HTTP 验证记录
scripts/        本地启动和 HTTP 验证脚本
```

项目设计：[需求与验收](01_需求与验收.md)、[架构与数据库](03_架构与数据库设计.md)、[接口约定](04_接口约定.md)。

## 运行环境

| 工具 | 版本 |
|---|---|
| Java | JDK 8 |
| Maven | 3.5+，实际验证为 3.9.16 |
| MySQL | 8.0.16+，InnoDB、utf8mb4；实际验证为 8.0.46 |
| Spring Boot | 2.7.18 |
| MyBatis Starter | 2.3.2 |
| 接口定义 | springdoc-openapi 1.8.0 |

为满足 JDK 8 约束使用 Boot 2.7.18。该版本的 [Java 兼容要求](https://docs.spring.io/spring-boot/docs/2.7.18/reference/html/getting-started.html#getting-started.system-requirements)符合本项目，但 Spring Boot 2.x 的[开源支持已经结束](https://spring.io/blog/2023/11/23/spring-boot-2-7-18-available-now/)。当前交付适用于本地考核演示，不应未经升级、安全评估和配置加固就部署到公网。

## 1. 当前本机数据库

这个原后端项目现在默认连接已经在使用的 `127.0.0.1:3306/exam_system_fullstack`，用户名为 `root`。密码不保存在项目中，启动时通过本机提示输入。`spring.sql.init.mode=never`，所以应用启动不会自动建表、导入种子数据或清空现有数据。

当前库已经初始化，不要再次执行 `database/01_schema.sql` 或 `database/02_demo_data.sql`。

### 仅在另一台机器首次创建新库时使用

使用有建库权限的 MySQL 账号，创建专用空库：

```sql
CREATE DATABASE exam_system_fullstack CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE exam_system_fullstack;
```

只对新建的空库依次执行：

1. [01_schema.sql](database/01_schema.sql)
2. [02_demo_data.sql](database/02_demo_data.sql)

脚本不删除已有表或数据，也不会在应用启动时自动执行。若数据库已存在，先确认其用途，不要覆盖或清空旧库。

正式部署建议创建只供项目使用的运行账号。下面的密码是占位文本，执行前必须替换：

```sql
CREATE USER 'exam_app'@'localhost' IDENTIFIED BY '替换为你设置的数据库密码';
GRANT SELECT, INSERT, UPDATE ON exam_system_fullstack.* TO 'exam_app'@'localhost';
```

建表、演示数据初始化与集成测试使用另外的管理/测试账号；运行账号不需要全局管理权限。

## 2. 编译与启动

在 `backend` 目录执行：

```powershell
mvn package
```

成功后得到 `backend/target/exam-system-0.1.0.jar`。回到本项目目录执行：

```powershell
.\scripts\start.ps1
```

脚本默认连接当前的 `exam_system_fullstack`，并安全提示输入 MySQL `root` 密码；密码不会写入项目文件或命令参数，退出时还会恢复当前进程原来的环境变量。这里输入的是数据库密码，不是下文的演示登录密码。

也可以在 IDEA 打开 [backend/pom.xml](backend/pom.xml)，项目 SDK 设为 JDK 8，为运行配置设置 `DB_USERNAME`、`DB_PASSWORD`（必要时设置 `DB_URL`），然后运行 `ExamApplication`。

默认地址：

- 后端：`http://127.0.0.1:3636`
- 健康检查：`GET /api/health`（只表示应用已启动，不等于数据库可用）
- 在线接口页：`http://127.0.0.1:3636/swagger-ui.html`
- OpenAPI：`http://127.0.0.1:3636/v3/api-docs`

应用默认只监听本机。启动脚本可用 `-Port` 指定端口、`-DatabaseUrl` 指定连接地址。项目不包含任何个人数据库密码。

## 3. 演示账号

以下均为虚构演示账号，共用公开演示密码：`Exam@2026!`。

| 账号 | 角色 | 关联课程 |
|---|---|---|
| teacher01 | 教师 | Java 基础、空题库演示 |
| teacher02 | 教师 | SQL 基础 |
| student01 | 学生 | Java 基础、空题库演示 |
| student02 | 学生 | Java 基础、SQL 基础 |
| student03 | 学生 | SQL 基础 |

Java 演示卷有两题，共 30 分；SQL 演示卷有一题，共 15 分。数据库保存 BCrypt 哈希，不保存明文应用密码。这些公开演示账号不可用于生产环境。

## 4. 前端与 Apifox

先调用 `GET /api/auth/csrf` 获取令牌，再携带 Cookie 和 `X-CSRF-Token` 登录。登录后必须改用登录响应中的新令牌。所有写请求均须携带当前令牌。

前端开发地址默认允许 `http://localhost:5173` 和 `http://127.0.0.1:5173`；跨域请求需要携带凭据，例如 `fetch` 的 `credentials: 'include'`。不要混用 `localhost` 和 `127.0.0.1` 的会话 Cookie。

前端地址不同可配置 `CORS_ALLOWED_ORIGINS`，多个精确地址用逗号分隔；不要配置通配符。跨机器联调需明确设置 `SERVER_ADDRESS`、防火墙和允许的前端源。公网部署还需要 HTTPS、安全 Cookie、限流、账号管理、审计及受维护的依赖版本。

导入及案例说明见 [api/README.md](api/README.md)。本地 OpenAPI 和响应文件已经生成，但尚未导入团队 Apifox，也尚未进行前端页面联调。

## 5. 验证

在 `backend` 目录执行 `mvn test`，默认使用内存 H2 数据库，不连接本机 MySQL。H2 只用于测试，不进入正式运行包。

真实 MySQL 集成测试和 HTTP 验证说明见 [TESTING.md](TESTING.md)。2026-09-05 最新重构已通过 39 项 H2 测试并完成 JDK 8 打包；新增验证包含答案乱序、判分结果不可修改及接口文档跟随当前主机和端口。

以下为初版验证记录：

- JDK 8 编译及可运行 JAR 打包成功。
- 36 项测试在 H2 和 MySQL 8.0.46 上分别通过。
- 19 次真实 HTTP 请求完成正常考试、重考和权限/会话检查。

初版使用独立临时 MySQL 实例；本次重构测试使用内存 H2，均未修改既有业务数据。业务源码仅在本地生成和修改，没有自动提交或推送 GitHub。

## 主要业务约束

- 初版为四选一单选题；一门课程的有效题目组成试卷，最多 1000 题。
- 一次开考保存题干、选项、答案和分值快照，之后修改题库不影响旧卷。
- 身份取自服务端会话；学生只能查看本人答卷，教师只能管理本人的课程。
- 服务端完整校验答案后判分；一次交卷事务包含明细和总分的全部写入。
- 重复交卷返回第一次保存的结果；重考必须新建答卷，旧成绩不覆盖。
- 未实现公开注册、自主选课、限时考试、复杂题型或正式前端页面。
