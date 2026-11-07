package com.winter.cloud.datasource.domain.dto;

import lombok.Data;

/**
 * 作业同步执行结果
 */
@Data
public class DsJobSyncResult {

    /** 源侧拉取行数 */
    private int sourceCount;

    /** 成功写入目标行数 */
    private int insertedCount;

    /** 写入失败行数 */
    private int errorCount;

    /** 自动新建表 */
    private boolean tableCreated;

    /** 自动新增列数量 */
    private int columnsAdded;

    /** 摘要信息 */
    private String message;
}
