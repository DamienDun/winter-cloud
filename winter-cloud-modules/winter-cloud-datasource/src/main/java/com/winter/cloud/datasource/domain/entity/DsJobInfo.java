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
 * 数据源作业信息（绑定 xxl_job_id）
 */
@Getter
@Setter
@NoArgsConstructor
@TableName("ds_job_info")
@Schema(description = "数据源作业信息")
public class DsJobInfo extends DefaultBaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "绑定的 xxl_job_info.id")
    private Long xxlJobId;

    @Schema(description = "作业名称")
    private String jobName;

    /** 0停用 1开启 2过期 */
    @Schema(description = "作业状态")
    private Integer jobStatus;

    /** -1未执行 0执行中 1成功 2失败 3异常 */
    @Schema(description = "最近执行状态")
    private Integer lastExecuteStatus;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "最近执行时间")
    private Date lastExecuteTime;

    @Schema(description = "数据源id")
    private Long datasourceId;

    @Schema(description = "数据源名称")
    private String datasourceName;

    @Schema(description = "数据源类型")
    private String datasourceType;

    /** 0全量 1增量 */
    @Schema(description = "同步方式")
    private Integer syncType;

    /** 0手动 1定时 */
    @Schema(description = "触发方式")
    private Integer triggerType;

    @Schema(description = "cron表达式")
    private String cron;

    @Schema(description = "cron配置")
    private String cronSetting;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "生效开始时间")
    private Date effectiveTimeBegin;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "生效结束时间")
    private Date effectiveTimeEnd;
}
