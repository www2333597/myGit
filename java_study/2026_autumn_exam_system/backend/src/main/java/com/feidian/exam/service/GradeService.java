/* 文件导读：成绩读取业务，与纯计算 GradingService 区分。最新列表和历史列表都只包含已交卷记录，权限和分页必须先落实到数据库查询条件。 */
// package 划分命名空间与职责；Spring 的组件扫描范围从启动类所在包开始。
package com.feidian.exam.service;

// import 引入下面使用的类型/静态方法名，不会在这里创建对象、连接数据库或执行请求。
import com.feidian.exam.common.*;
import com.feidian.exam.dto.response.GradeResponse;
import com.feidian.exam.mapper.GradeMapper;
import com.feidian.exam.security.SessionUser;
import org.springframework.stereotype.Service;

/**
 * 成绩查询与判分分开：本类只查已保存的成绩，GradingService 才负责计算分数。
 * latest 是每个学生最近一次已交卷成绩，不是最高分；history 返回所有已交卷记录。
 */
@Service
public class GradeService {
    /** 已保存成绩的查询依赖。 */
    private final GradeMapper grades;
    /** 课程角色与资源归属检查组件。 */
    private final CourseAccess access;

    /** 注入成绩 Mapper 和课程授权组件。 */
    public GradeService(GradeMapper grades, CourseAccess access) {
        this.grades = grades;
        this.access = access;
    }

    /** 验证教师拥有课程，去掉姓名首尾空格并限制长度；列表/count 使用相同过滤，返回每名学生最近一次成绩。 */
    public PageResponse<GradeResponse> latest(SessionUser teacher, Long courseId, String name, PageQuery page) {
        access.teacherCourse(teacher, courseId);
        String filter = name == null ? null : name.trim();
        if (filter != null && filter.length() > 50) {
            throw BusinessException.badRequest("姓名筛选最长 50 字符");
        }
        return new PageResponse<>(grades.latest(teacher.id, courseId, filter, page),
                grades.countLatest(teacher.id, courseId, filter), page);
    }

    /** 先验证教师拥有课程，再查询指定学生的已交卷历史；此处没有公开学生跨课程查询接口。 */
    public PageResponse<GradeResponse> teacherHistory(SessionUser teacher, Long courseId, Long studentId, PageQuery page) {
        access.teacherCourse(teacher, courseId);
        return history(studentId, courseId, page);
    }

    /** 先验证学生已选课，再将 Session 学生 ID 传给历史查询，不能从客户端接收其他学生 ID。 */
    public PageResponse<GradeResponse> studentHistory(SessionUser student, Long courseId, PageQuery page) {
        access.studentCourse(student, courseId);
        return history(student.id, courseId, page);
    }

    /** 公共的内部查询步骤：拿当前页列表和总记录数，组装 PageResponse；private 方法不暴露为 HTTP。 */
    private PageResponse<GradeResponse> history(Long studentId, Long courseId, PageQuery page) {
        return new PageResponse<>(grades.history(studentId, courseId, page), grades.countHistory(studentId, courseId), page);
    }
}
