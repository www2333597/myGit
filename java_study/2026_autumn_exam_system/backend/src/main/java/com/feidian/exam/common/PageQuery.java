/* 文件导读：所有分页参数的统一边界。先校验再做乘法，避免负 OFFSET、超大页面以及 int 溢出；它不是 MyBatis 分页插件，LIMIT/OFFSET 仍由 XML 明确写出。 */
// package 划分命名空间与职责；Spring 的组件扫描范围从启动类所在包开始。
package com.feidian.exam.common;

/** 将页码统一换算为 SQL OFFSET，并提前限制范围，防止超大分页和 int 乘法溢出。 */
public class PageQuery {
    /** 从 1 开始的页码。 */
    public final int page;
    /** 每页条数，合法范围由 PageQuery 检查。 */
    public final int pageSize;
    /** SQL 从 0 开始的偏移量。 */
    public final int offset;

    /** 接收 page/pageSize，限定 1–100000 页、每页 1–50 条，再计算 offset=(page-1)*pageSize；页码从 1 开始，SQL 偏移从 0 开始。 */
    public PageQuery(int page, int pageSize) {
        if (page < 1 || page > 100000 || pageSize < 1 || pageSize > 50) {
            throw BusinessException.badRequest("page 须为 1–100000，pageSize 须为 1–50");
        }
        this.page = page;
        this.pageSize = pageSize;
        // 由于 page <= 100000 且 pageSize <= 50，这里的乘积不可能超过 int 上限。
        this.offset = (page - 1) * pageSize;
    }
}
