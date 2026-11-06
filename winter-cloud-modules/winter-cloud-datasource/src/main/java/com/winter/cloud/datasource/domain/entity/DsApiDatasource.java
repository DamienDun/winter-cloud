package com.winter.cloud.datasource.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.winter.cloud.common.mybatis.domain.entity.DefaultBaseEntity;
import com.winter.cloud.datasource.domain.dto.DsApiReqParams;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * API 数据源配置
 */
@Getter
@Setter
@NoArgsConstructor
@TableName("ds_api_datasource")
@Schema(description = "API数据源")
public class DsApiDatasource extends DefaultBaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "数据源名称")
    private String datasourceName;

    @Schema(description = "数据源分组")
    private String datasourceGroup;

    @Schema(description = "接口地址")
    private String apiUrl;

    /** 0 POST，1 GET */
    @Schema(description = "请求方式：0 POST，1 GET")
    private Integer reqType;

    @Schema(description = "数据项KEY（响应取值路径）")
    private String dataItemKey;

    @Schema(description = "请求参数JSON")
    private String reqParamsJson;

    /** 非表字段：解析后的请求参数对象 */
    @TableField(exist = false)
    @Schema(description = "请求参数对象")
    private DsApiReqParams reqParams;

    /** 依赖API主键：序列化为字符串，避免前端雪花精度丢失 */
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "依赖API数据源ID")
    private Long relyDatasourceId;

    /** 0 依赖其它数据源，1 普通（不依赖） */
    @Schema(description = "是否依赖数据源：0是，1否")
    private Integer belongToRelyDatasource;

    @Schema(description = "数据源描述")
    private String datasourceDesc;

    /** 0 停用，1 启用 */
    @Schema(description = "状态：0停用，1启用")
    private Integer status;
}
