/* 文件导读：MyBatis ExamMapper 代理接口；namespace 对应全类名、XML 的 id 对应方法名。@Param 明确多个参数的名称，#{} 绑定值；查询结果通过类型处理器和驼峰规则映射，写方法的 int 表示影响行数。 */
// package 划分命名空间与职责；Spring 的组件扫描范围从启动类所在包开始。
package com.feidian.exam.mapper;

// import 引入下面使用的类型/静态方法名，不会在这里创建对象、连接数据库或执行请求。
import com.feidian.exam.model.*;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * MyBatis Mapper 只声明数据库操作，运行时由框架生成代理对象并匹配 ExamMapper.xml。
 * @Param 的名字要和 XML 中的 #{studentId}、#{courseId} 等占位符一致。
 */
public interface ExamMapper {
    /** 查询学生在课程中 IN_PROGRESS 答卷，用于重复开考时复用；调用前已经锁定该选课记录。 */
    Attempt active(@Param("studentId") Long studentId, @Param("courseId") Long courseId);
    /** MAX 取得最大考试序号；没有记录时聚合为 null，COALESCE 转为 0，业务层再加 1。 */
    Integer lastAttemptNo(@Param("studentId") Long studentId, @Param("courseId") Long courseId);
    /** 插入答卷头；useGeneratedKeys/keyProperty 从 JDBC 获取 MySQL 自增主键并写回 Attempt.id，不是 insert 方法返回 ID。 */
    int insertAttempt(Attempt attempt);
    /** 插入本次试题快照与来源 ID；题干、四选项、正确答案、分值共同冻结，和卷头在一个事务内保存。 */
    int insertItem(AttemptItem item);
    /** 同时按答卷 ID 和会话学生 ID 查询，JOIN 课程名称用于响应；避免先查询任意卷再忘记检查归属。 */
    Attempt findOwned(@Param("id") Long id, @Param("studentId") Long studentId);
    /** 按答卷主键及所有者执行锁定读 FOR UPDATE，让同卷提交串行化；后来的事务可看到已提交状态。 */
    Attempt lockOwned(@Param("id") Long id, @Param("studentId") Long studentId);
    /** 按答卷 ID 查询全部快照，ORDER BY position_no 保证卷面顺序；学生响应会在 Service 中过滤正确答案字段。 */
    List<AttemptItem> items(@Param("attemptId") Long attemptId);
    /** UPDATE 一道明细的学生选项及得分；同时限制 id/attempt_id，返回影响行数供事务校验。 */
    int saveAnswer(AttemptItem item);
    /** 一次更新总分、SUBMITTED 状态和提交时间；WHERE 还要求原状态 IN_PROGRESS，保护合法状态迁移。 */
    int submit(Attempt attempt);
}
