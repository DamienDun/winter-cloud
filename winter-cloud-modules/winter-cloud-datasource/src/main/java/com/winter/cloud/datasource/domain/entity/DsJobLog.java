package com.winter.cloud.datasource.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.winter.cloud.common.mybatis.domain.entity.DefaultBaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

/**
 * 数据源作业执行日志
 */
@Getter
@Setter
@NoArgsConstructor
@TableName("ds_job_log")
@Schema(description = "数据源作业日志")
public class DsJobLog extends DefaultBaseEntity {

    private static final long serialVersionUID = 1L;

    /** 业务作业 ID（雪花）；字符串输出，避免前端精度丢失导致查不到日志 */
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "作业ID")
    private Long jobId;

    /** 作业名称（冗余） */
    @Schema(description = "作业名称")
    private String jobName;

    /** 触发方式：0 手动，1 定时 */
    @Schema(description = "触发方式：0手动，1定时")
    private Integer triggerType;

    /** 触发人用户 ID（手动触发时有值） */
    @Schema(description = "触发人用户ID")
    private Long triggerUserId;

    /** 触发人用户名 */
    @Schema(description = "触发人用户名")
    private String triggerUsername;

    /** 执行状态：0 执行中，1 成功，2 失败 */
    @Schema(description = "执行状态：0执行中，1成功，2失败")
    private Integer executeStatus;

    /** 执行开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "执行开始时间")
    private Date executeBeginTime;

    /** 执行结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "执行结束时间")
    private Date executeEndTime;

    /** 耗时描述（如 1.2s / 320ms） */
    @Schema(description = "耗时")
    private String timeConsuming;

    /** 源侧处理/拉取的数据总量 */
    @Schema(description = "处理数据总量")
    private Integer handleDataTotal;

    /** 异常/失败数据总量 */
    @Schema(description = "异常数据总量")
    private Integer errorDataTotal;

    /** 实际写入/更新到目标的数据总量 */
    @Schema(description = "实际更新数据总量")
    private Integer actualUpdateDataTotal;

    /** 作业日志正文（含分步详情） */
    @Schema(description = "日志信息")
    private String jobMessage;

    /** XXL-JOB 本次调度日志 ID，终止任务时回传调度中心 */
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "XXL调度日志ID")
    private Long xxlLogId;

    /** 源数据源类型（冗余：api/mysql 等） */
    @Schema(description = "数据源类型")
    private String datasourceType;
}
