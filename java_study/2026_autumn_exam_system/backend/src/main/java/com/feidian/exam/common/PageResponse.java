/* 文件导读：分页响应泛型容器。items 是当前页数据，total 是满足筛选条件的总记录数，不能用 items.size() 代替 total。final List 只固定引用，不代表列表深不可变。 */
// package 划分命名空间与职责；Spring 的组件扫描范围从启动类所在包开始。
package com.feidian.exam.common;

// import 引入下面使用的类型/静态方法名，不会在这里创建对象、连接数据库或执行请求。
import java.util.List;

public class PageResponse<T> {
    /** 当前页数据集合。 */
    public final List<T> items;
    /** 筛选后的总记录数，不是当前页条数。 */
    public final long total;
    /** 从 1 开始的页码。 */
    public final int page;
    /** 每页条数，合法范围由 PageQuery 检查。 */
    public final int pageSize;

    /** 把查询列表、COUNT 结果和经过校验的页码信息组合起来，供 Jackson 输出 JSON；不在响应类里执行 SQL。 */
    public PageResponse(List<T> items, long total, PageQuery query) {
        this.items = items;
        this.total = total;
        this.page = query.page;
        this.pageSize = query.pageSize;
    }
}
