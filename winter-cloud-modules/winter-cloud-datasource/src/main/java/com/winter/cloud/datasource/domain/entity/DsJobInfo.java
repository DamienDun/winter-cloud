package com.winter.cloud.datasource.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.winter.cloud.common.mybatis.domain.entity.DefaultBaseEntity;
import com.winter.cloud.datasource.domain.dto.DsJobFieldMapping;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;
import java.util.List;

/**
 * 数据源作业信息（绑定 XXL-JOB 任务）
 */
@Getter
@Setter
@NoArgsConstructor
@TableName("ds_job_info")
@Schema(description = "数据源作业信息")
public class DsJobInfo extends DefaultBaseEntity {

    private static final long serialVersionUID = 1L;

    /** 绑定的 XXL-JOB 任务 ID（xxl_job_info.id） */
    @Schema(description = "绑定的XXL-JOB任务ID")
    private Long xxlJobId;

    /** 作业名称 */
    @Schema(description = "作业名称")
    private String jobName;

    /** 作业状态：0 停用，1 开启，2 过期 */
    @Schema(description = "作业状态：0停用，1开启，2过期")
    private Integer jobStatus;

    /** 最近执行状态：-1 未执行，0 执行中，1 成功，2 失败，3 异常 */
    @Schema(description = "最近执行状态：-1未执行，0执行中，1成功，2失败，3异常")
    private Integer lastExecuteStatus;

    /** 最近执行时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "最近执行时间")
    private Date lastExecuteTime;

    /** 源数据源 ID（API 或 JDBC） */
    @Schema(description = "源数据源ID")
    private Long datasourceId;

    /** 源数据源名称（冗余展示） */
    @Schema(description = "源数据源名称")
    private String datasourceName;

    /** 源数据源类型：api / mysql / oracle 等 */
    @Schema(description = "源数据源类型：api/mysql/oracle等")
    private String datasourceType;

    /** 源表名（仅 JDBC 源使用；API 源可空） */
    @Schema(description = "源表（JDBC源时使用）")
    private String sourceTable;

    /** 目标 JDBC 数据源 ID */
    @Schema(description = "目标数据源ID")
    private Long targetDatasourceId;

    /** 目标数据源名称（冗余展示） */
    @Schema(description = "目标数据源名称")
    private String targetDatasourceName;

    /** 目标数据表名 */
    @Schema(description = "目标数据表")
    private String targetTable;

    /** 字段映射 JSON 原文（入库字段） */
    @Schema(description = "字段映射JSON")
    private String fieldMappingJson;

    /** 非表字段：由 fieldMappingJson 解析出的字段映射列表（顺序即建表/同步列顺序） */
    @TableField(exist = false)
    @Schema(description = "字段映射列表")
    private List<DsJobFieldMapping> fieldMappings;

    /** 同步方式：0 全量，1 增量 */
    @Schema(description = "同步方式：0全量，1增量")
    private Integer syncType;

    /** 触发方式：0 手动，1 定时 */
    @Schema(description = "触发方式：0手动，1定时")
    private Integer triggerType;

    /** Cron 表达式（定时触发时必填） */
    @Schema(description = "Cron表达式")
    private String cron;

    /** Cron 前端配置原文（可选，便于回显可视化配置） */
    @Schema(description = "Cron配置（前端可视化原文，可选）")
    private String cronSetting;

    /** 作业生效开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "生效开始时间")
    private Date effectiveTimeBegin;

    /** 作业生效结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "生效结束时间")
    private Date effectiveTimeEnd;
}
