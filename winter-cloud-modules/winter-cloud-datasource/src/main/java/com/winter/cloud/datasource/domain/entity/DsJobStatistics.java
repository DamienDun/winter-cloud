package com.winter.cloud.datasource.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.winter.cloud.common.mybatis.domain.entity.DefaultBaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

/**
 * 数据源作业日统计
 */
@Getter
@Setter
@NoArgsConstructor
@TableName("ds_job_statistics")
@Schema(description = "数据源作业统计")
public class DsJobStatistics extends DefaultBaseEntity {

    private static final long serialVersionUID = 1L;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "统计日期")
    private Date dateTime;

    @Schema(description = "待执行任务数")
    private Integer pendingJob;

    @Schema(description = "已执行次数")
    private Integer executed;

    @Schema(description = "失败作业个数")
    private Integer failedJob;

    @Schema(description = "失败执行次数")
    private Integer failedExecution;

    @Schema(description = "处理数据总量")
    private Integer handleDataTotal;

    @Schema(description = "实际更新数据总量")
    private Integer actualUpdateDataTotal;

    @Schema(description = "异常数据总量")
    private Integer errorDataTotal;
}
