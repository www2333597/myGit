/* 文件导读：MyBatis CourseMapper 代理接口；namespace 对应全类名、XML 的 id 对应方法名。@Param 明确多个参数的名称，#{} 绑定值；查询结果通过类型处理器和驼峰规则映射，写方法的 int 表示影响行数。 */
// package 划分命名空间与职责；Spring 的组件扫描范围从启动类所在包开始。
package com.feidian.exam.mapper;

// import 引入下面使用的类型/静态方法名，不会在这里创建对象、连接数据库或执行请求。
import com.feidian.exam.model.Course;
import com.feidian.exam.dto.response.StudentCourseResponse;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface CourseMapper {
    /** 按课程主键查询并 JOIN 教师姓名；单条返回 Course，找不到为 null，Service 决定是否抛 404。 */
    Course findById(@Param("id") Long id);
    /** WHERE teacher_id 使用 Session 中的教师 ID，ORDER BY c.id 保证列表顺序稳定。 */
    List<Course> teacherCourses(@Param("teacherId") Long teacherId);
    /** 通过 enrollment 关联学生和课程，再 JOIN 教师姓名；学生与课程是多对多关系。 */
    List<Course> studentCourses(@Param("studentId") Long studentId);
    /** 一次联表查询组装概览：CASE 优先显示进行中状态，LEFT JOIN 保留尚未考试的课程，NOT EXISTS 选择最近已交卷记录。 */
    List<StudentCourseResponse> studentOverview(@Param("studentId") Long studentId);
    /** COUNT 检查学生/课程组合是否存在；唯一约束保证合法数据最多 1 条，用于资源权限判断。 */
    int isEnrolled(@Param("studentId") Long studentId, @Param("courseId") Long courseId);
    /** 对稳定的选课记录执行 FOR UPDATE，返回被锁行主键；必须位于 ExamService.start 的事务内，锁持有到事务结束。 */
    Long lockEnrollment(@Param("studentId") Long studentId, @Param("courseId") Long courseId);
}
