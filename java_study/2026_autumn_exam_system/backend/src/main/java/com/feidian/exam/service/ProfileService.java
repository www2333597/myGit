/* 文件导读：本人资料业务 Bean。ProfileUpdateRequest 只开放四个字段，UserMapper.updateProfile 的 WHERE 使用 Session 中的用户 ID；事务统一保护更新和回查。 */
// package 划分命名空间与职责；Spring 的组件扫描范围从启动类所在包开始。
package com.feidian.exam.service;

// import 引入下面使用的类型/静态方法名，不会在这里创建对象、连接数据库或执行请求。
import com.feidian.exam.common.BusinessException;
import com.feidian.exam.dto.request.ProfileUpdateRequest;
import com.feidian.exam.dto.response.*;
import com.feidian.exam.mapper.*;
import com.feidian.exam.model.*;
import com.feidian.exam.security.SessionUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/** 个人资料以 Session 中的用户 ID 为查询和修改条件；响应 DTO 排除密码哈希。 */
@Service
public class ProfileService {
    /** 用户 Mapper 代理，执行账号/资料 SQL。 */
    private final UserMapper users;
    /** 课程与选课关系 Mapper 代理。 */
    private final CourseMapper courses;
    /** 课程角色与资源归属检查组件。 */
    private final CourseAccess access;

    /** 构造器注入用户/课程 Mapper 和课程权限组件。 */
    public ProfileService(UserMapper users, CourseMapper courses, CourseAccess access) {
        this.users = users;
        this.courses = courses;
        this.access = access;
    }

    /** 按会话 ID 查用户，按教师/学生角色查询授课或选课列表，组合 UserResponse；原始 User.passwordHash 不输出。 */
    public UserResponse profile(SessionUser current) {
        User user = users.findById(current.id);
        if (user == null) {
            throw BusinessException.notFound();
        }
        List<Course> associated = "TEACHER".equals(current.role)
                ? courses.teacherCourses(current.id) : courses.studentCourses(current.id);
        return new UserResponse(user, associated);
    }

    @Transactional(rollbackFor = Exception.class)
    /** 去除姓名、学院、电话首尾空格，更新行数需为 1，随后回查最终资料；事务使失败时更新回滚。 */
    public UserResponse update(SessionUser current, ProfileUpdateRequest request) {
        // 请求 DTO 与 SQL 只开放姓名、性别、手机、学院；角色、学工号不能通过此接口更改。
        request.setRealName(request.getRealName().trim());
        request.setCollege(request.getCollege().trim());
        request.setPhone(request.getPhone().trim());
        if (users.updateProfile(current.id, request) != 1) {
            throw BusinessException.notFound();
        }
        return profile(current);
    }

    /** 必须是教师，再按 teacher_id 查询本人的课程列表。 */
    public List<Course> teacherCourses(SessionUser current) {
        access.requireRole(current, "TEACHER");
        return courses.teacherCourses(current.id);
    }

    /** 必须是学生，调用单条概览 SQL 同时获得课程、当前考试和最近成绩。 */
    public List<StudentCourseResponse> studentCourses(SessionUser current) {
        access.requireRole(current, "STUDENT");
        return courses.studentOverview(current.id);
    }
}
