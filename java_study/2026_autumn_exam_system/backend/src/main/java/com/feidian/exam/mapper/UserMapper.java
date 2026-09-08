/* 文件导读：MyBatis UserMapper 代理接口；namespace 对应全类名、XML 的 id 对应方法名。@Param 明确多个参数的名称，#{} 绑定值；查询结果通过类型处理器和驼峰规则映射，写方法的 int 表示影响行数。 */
// package 划分命名空间与职责；Spring 的组件扫描范围从启动类所在包开始。
package com.feidian.exam.mapper;

// import 引入下面使用的类型/静态方法名，不会在这里创建对象、连接数据库或执行请求。
import com.feidian.exam.model.User;
import com.feidian.exam.dto.request.ProfileUpdateRequest;
import org.apache.ibatis.annotations.Param;

public interface UserMapper {
    /** 使用 username 绑定参数查账号，包括仅供服务端核验的 password_hash；Java 层用 BCrypt.matches 判断密码。 */
    User findByUsername(@Param("username") String username);
    /** 按用户主键查询完整数据库对象；向客户端返回前必须转换为 UserResponse 白名单。 */
    User findById(@Param("id") Long id);
    /** 只更新 real_name/gender/phone/college；#{id} 来自 Session，#{profile.xxx} 对应 @Param("profile") 的 DTO 属性。 */
    int updateProfile(@Param("id") Long id, @Param("profile") ProfileUpdateRequest profile);
}
