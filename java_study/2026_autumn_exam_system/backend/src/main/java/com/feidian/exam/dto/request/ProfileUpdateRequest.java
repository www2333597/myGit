/* 文件导读：资料修改白名单 DTO：仅允许姓名、性别、手机、学院。没有 role/id/identityNo 字段，禁止通过修改资料伪造身份。手机号允许空字符串，表示未填写。 */
// package 划分命名空间与职责；Spring 的组件扫描范围从启动类所在包开始。
package com.feidian.exam.dto.request;

// import 引入下面使用的类型/静态方法名，不会在这里创建对象、连接数据库或执行请求。
import javax.validation.constraints.*;

public class ProfileUpdateRequest {
    // @NotBlank 同时拒绝 null、空串及纯空白；与允许空串的 @NotNull 不同。
    @NotBlank @Size(max = 50)
    /** realName：真实姓名/演示姓名。 */
    private String realName;
    // 性别字段必须存在，正则只允许 UNKNOWN/MALE/FEMALE 三个业务值。
    @NotNull @Pattern(regexp = "UNKNOWN|MALE|FEMALE")
    /** gender：性别枚举 UNKNOWN/MALE/FEMALE。 */
    private String gender;
    // 电话不能为 null，但长度 0–20 允许空串；只接收数字、加号、减号和空格，不强制 11 位。
    @NotNull @Pattern(regexp = "[0-9+\\- ]{0,20}")
    /** phone：电话号码文本，保留前导零和加号。 */
    private String phone;
    // @NotBlank 同时拒绝 null、空串及纯空白；与允许空串的 @NotNull 不同。
    @NotBlank @Size(max = 100)
    /** college：所属学院。 */
    private String college;

    /** 读取真实姓名/演示姓名；供 Service 取出已绑定的请求值。 */
    public String getRealName() { return realName; }
    /** 设置真实姓名/演示姓名；由 Jackson 绑定请求或由业务层规范化，此方法本身不执行 SQL。 */
    public void setRealName(String realName) { this.realName = realName; }

    /** 读取性别枚举 UNKNOWN/MALE/FEMALE；供 Service 取出已绑定的请求值。 */
    public String getGender() { return gender; }
    /** 设置性别枚举 UNKNOWN/MALE/FEMALE；由 Jackson 绑定请求或由业务层规范化，此方法本身不执行 SQL。 */
    public void setGender(String gender) { this.gender = gender; }

    /** 读取电话号码文本，保留前导零和加号；供 Service 取出已绑定的请求值。 */
    public String getPhone() { return phone; }
    /** 设置电话号码文本，保留前导零和加号；由 Jackson 绑定请求或由业务层规范化，此方法本身不执行 SQL。 */
    public void setPhone(String phone) { this.phone = phone; }

    /** 读取所属学院；供 Service 取出已绑定的请求值。 */
    public String getCollege() { return college; }
    /** 设置所属学院；由 Jackson 绑定请求或由业务层规范化，此方法本身不执行 SQL。 */
    public void setCollege(String college) { this.college = college; }
}
