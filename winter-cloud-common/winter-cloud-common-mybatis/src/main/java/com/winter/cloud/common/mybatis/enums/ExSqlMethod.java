package com.winter.cloud.common.mybatis.enums;

/** 扩展 SQL 方法 */
public enum ExSqlMethod {
    INSERT_BATCH("insertBatch", "批量插入数据", "<script>insert into %s %s values %s</script>"),
    REPLACE_BATCH("replaceBatch", "批量替换数据", "<script>replace into %s %s values %s</script>"),
    UPDATE_BATCH_BY_ID("updateBatchById", "通过主键批量更新数据", "<script>UPDATE %s \n%s \nWHERE %s IN %s\n</script>");

    private final String method;
    private final String desc;
    private final String sql;

    ExSqlMethod(String method, String desc, String sql) {
        this.method = method;
        this.desc = desc;
        this.sql = sql;
    }

    public String getMethod() { return method; }
    public String getDesc() { return desc; }
    public String getSql() { return sql; }
}
