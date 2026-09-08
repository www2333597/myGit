/* 文件导读：个人资料响应白名单：从 sys_user 和关联课程组装。公开 final 字段能被 Jackson 序列化；final 只固定引用，courses 列表并非深不可变。 */
// package 划分命名空间与职责；Spring 的组件扫描范围从启动类所在包开始。
package com.feidian.exam.dto.response;

// import 引入下面使用的类型/静态方法名，不会在这里创建对象、连接数据库或执行请求。
import com.feidian.exam.model.Course;
import com.feidian.exam.model.User;
import java.util.List;

public class UserResponse {
    /** id：当前记录的 MySQL 主键（BIGINT 对应 Long）。 */
    public final Long id;
    /** college：所属学院。 */
    public final String role, realName, gender, phone, identityNo, college;
    /** courses：当前用户关联的课程列表。 */
    public final List<Course> courses;

    /** 构造当前响应并显式复制允许输出的字段；不执行数据库查询或判分。 */
    public UserResponse(User user, List<Course> courses) {
        this.id = user.getId();
        this.role = user.getRole();
        this.realName = user.getRealName();
        this.gender = user.getGender();
        this.phone = user.getPhone();
        this.identityNo = user.getIdentityNo();
        this.college = user.getCollege();
        this.courses = courses;
    }
}
