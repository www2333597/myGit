/* 文件导读：可复用资源授权组件。身份校验回答“你是谁/角色是什么”，课程归属校验回答“这门课程允许你操作吗”；MySQL 外键只保证 ID 存在，不能替代角色业务检查。 */
// package 划分命名空间与职责；Spring 的组件扫描范围从启动类所在包开始。
package com.feidian.exam.service;

// import 引入下面使用的类型/静态方法名，不会在这里创建对象、连接数据库或执行请求。
import com.feidian.exam.common.BusinessException;
import com.feidian.exam.mapper.CourseMapper;
import com.feidian.exam.model.Course;
import com.feidian.exam.security.SessionUser;
import org.springframework.stereotype.Component;

/**
 * 可复用的资源权限检查：教师必须拥有课程，学生必须选过课程。
 * 用户 ID/角色来自已认证会话，而课程归属从数据库读取，不能相信请求里的身份声明。
 */
@Component
public class CourseAccess {
    /** 课程与选课关系 Mapper 代理。 */
    private final CourseMapper courses;

    /** 注入 CourseMapper，让课程信息与选课关系以数据库为准。 */
    public CourseAccess(CourseMapper courses) {
        this.courses = courses;
    }

    /** 校验会话角色；null 或角色不匹配抛 403，用 String.equals 比较值而非 == 比较对象引用。 */
    public void requireRole(SessionUser user, String role) {
        if (user == null || !role.equals(user.role)) {
            throw BusinessException.forbidden();
        }
    }

    /** 先检查教师角色，再查询课程并比较 teacher.id 与 teacher_id；通过后返回 Course 供后续业务复用。 */
    public Course teacherCourse(SessionUser teacher, Long courseId) {
        requireRole(teacher, "TEACHER");
        Course course = getCourse(courseId);
        if (!teacher.id.equals(course.getTeacherId())) {
            throw BusinessException.forbidden();
        }
        return course;
    }

    /** 先检查学生角色与课程存在，再检查 enrollment 的学生/课程组合；未选课返回 403。 */
    public Course studentCourse(SessionUser student, Long courseId) {
        requireRole(student, "STUDENT");
        Course course = getCourse(courseId);
        if (courses.isEnrolled(student.id, courseId) == 0) {
            throw BusinessException.forbidden();
        }
        return course;
    }

    /** 集中查询课程，不存在时抛 404；避免多个入口各自编写不同的资源不存在处理。 */
    private Course getCourse(Long courseId) {
        Course course = courses.findById(courseId);
        if (course == null) {
            throw BusinessException.notFound();
        }
        return course;
    }
}
