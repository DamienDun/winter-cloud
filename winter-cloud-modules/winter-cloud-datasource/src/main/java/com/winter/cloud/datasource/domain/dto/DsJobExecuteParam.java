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

    /** 0手动 1定时 */
    private Integer triggerType;

    private Long triggerUserId;

    private String triggerUsername;
}
