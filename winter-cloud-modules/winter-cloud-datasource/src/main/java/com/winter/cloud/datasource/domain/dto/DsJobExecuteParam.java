package com.winter.cloud.datasource.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 手动/定时触发时写入 XXL executorParam 的参数
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DsJobExecuteParam {

    /** 业务作业主键 */
    private Long jobId;

    /** 触发方式：0 手动，1 定时 */
    private Integer triggerType;

    /** 触发人用户 ID（手动触发时有值） */
    private Long triggerUserId;

    /** 触发人用户名 */
    private String triggerUsername;
}
