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
 * 数据源作业日志
 */
@Getter
@Setter
@NoArgsConstructor
@TableName("ds_job_log")
@Schema(description = "数据源作业日志")
public class DsJobLog extends DefaultBaseEntity {

    private static final long serialVersionUID = 1L;

    /** 雪花作业ID，字符串输出避免前端精度丢失导致按错ID查不到日志 */
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "作业id")
    private Long jobId;

    @Schema(description = "作业名称")
    private String jobName;

    /** 0手动 1定时 */
    @Schema(description = "触发方式")
    private Integer triggerType;

    @Schema(description = "触发人id")
    private Long triggerUserId;

    @Schema(description = "触发人名称")
    private String triggerUsername;

    /** 0执行中 1成功 2失败 */
    @Schema(description = "执行状态")
    private Integer executeStatus;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "开始时间")
    private Date executeBeginTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "结束时间")
    private Date executeEndTime;

    @Schema(description = "耗时")
    private String timeConsuming;

    @Schema(description = "处理数据总量")
    private Integer handleDataTotal;

    @Schema(description = "异常数据总量")
    private Integer errorDataTotal;

    @Schema(description = "实际更新数据总量")
    private Integer actualUpdateDataTotal;

    @Schema(description = "日志信息")
    private String jobMessage;

    @Schema(description = "数据源类型")
    private String datasourceType;
}
