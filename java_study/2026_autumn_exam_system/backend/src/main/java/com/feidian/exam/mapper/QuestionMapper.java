/* 文件导读：MyBatis QuestionMapper 代理接口；namespace 对应全类名、XML 的 id 对应方法名。@Param 明确多个参数的名称，#{} 绑定值；查询结果通过类型处理器和驼峰规则映射，写方法的 int 表示影响行数。 */
// package 划分命名空间与职责；Spring 的组件扫描范围从启动类所在包开始。
package com.feidian.exam.mapper;

// import 引入下面使用的类型/静态方法名，不会在这里创建对象、连接数据库或执行请求。
import com.feidian.exam.model.Question;
import com.feidian.exam.common.PageQuery;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface QuestionMapper {
    /** 按题库主键查有效题目；deleted=FALSE 排除逻辑删除，教师权限由 Service 依据 courseId 再检查。 */
    Question findById(@Param("id") Long id);
    /** 查询某课程有效题目的当前页；LIMIT/OFFSET 从 PageQuery 绑定，按 id 排序避免页面顺序漂移。 */
    List<Question> list(@Param("courseId") Long courseId, @Param("page") PageQuery page);
    /** 按与 list 相同的课程和删除条件计算总记录数；不应只用当前页大小作为 total。 */
    long count(@Param("courseId") Long courseId);
    /** 读取课程有效题目并按 id 固定排序；最多取 1001 条让 Service 识别超出 1000 题并拒绝，而不是截断试卷。 */
    List<Question> forExam(@Param("courseId") Long courseId);
    /** 按白名单字段新增题目并回填自增主键；courseId 来自已授权路径，时间/删除标志由数据库默认值提供。 */
    int insert(Question question);
    /** 更新题干、四选项、正确答案、分值及更新时间；沿用原题 ID/课程，并排除已删题。 */
    int update(Question question);
    /** HTTP DELETE 对应此逻辑删除 UPDATE；保留题库记录和历史外键，新的 forExam 查询排除该题。 */
    int delete(@Param("id") Long id, @Param("courseId") Long courseId);
}
