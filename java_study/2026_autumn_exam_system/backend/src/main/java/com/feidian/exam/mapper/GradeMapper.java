/* 文件导读：MyBatis GradeMapper 代理接口；namespace 对应全类名、XML 的 id 对应方法名。@Param 明确多个参数的名称，#{} 绑定值；查询结果通过类型处理器和驼峰规则映射，写方法的 int 表示影响行数。 */
// package 划分命名空间与职责；Spring 的组件扫描范围从启动类所在包开始。
package com.feidian.exam.mapper;

// import 引入下面使用的类型/静态方法名，不会在这里创建对象、连接数据库或执行请求。
import com.feidian.exam.common.PageQuery;
import com.feidian.exam.dto.response.GradeResponse;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface GradeMapper {
    /** 复用统一字段、JOIN、筛选，按提交时间和主键倒序分页；不是按 score 排序选最高分。 */
    List<GradeResponse> latest(@Param("teacherId") Long teacherId, @Param("courseId") Long courseId,
                              @Param("name") String name, @Param("page") PageQuery page);
    /** 与 latest 使用同一 latestWhere 片段，保证分页总数对应筛选后的最新成绩人数。 */
    long countLatest(@Param("teacherId") Long teacherId, @Param("courseId") Long courseId, @Param("name") String name);
    /** 只查询指定学生在课程内全部已交卷记录，按 attempt_no 倒序；Service 已确认访问权限。 */
    List<GradeResponse> history(@Param("studentId") Long studentId, @Param("courseId") Long courseId, @Param("page") PageQuery page);
    /** 统计相同学生/课程的已交卷记录总数；不包含正在重考但未交卷的记录。 */
    long countHistory(@Param("studentId") Long studentId, @Param("courseId") Long courseId);
}
