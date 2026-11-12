package com.winter.cloud.datasource.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.winter.cloud.common.mybatis.domain.entity.DefaultBaseEntity;
import com.winter.cloud.datasource.domain.dto.DsApiRelyItem;
import com.winter.cloud.datasource.domain.dto.DsApiReqParams;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

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

    /** 数据源名称 */
    @Schema(description = "数据源名称")
    private String datasourceName;

    /** 数据源分组，默认 Default */
    @Schema(description = "数据源分组")
    private String datasourceGroup;

    /**
     * 数据源类型：HTTP（默认，兼容旧数据） / WEBSERVICE（SOAP/XML）
     */
    @Schema(description = "数据源类型：HTTP / WEBSERVICE")
    private String datasourceType;

    /** 接口地址（HTTP 路径或 WebService 端点） */
    @Schema(description = "接口地址")
    private String apiUrl;

    /** 请求方式：0 POST，1 GET；WebService 固定为 POST */
    @Schema(description = "请求方式：0 POST，1 GET")
    private Integer reqType;

    /**
     * 数据项 KEY：响应中业务列表/对象的取值路径。
     * HTTP 为 JSON 点路径（如 data.list）；WebService 为 XML 点路径（如 Envelope.Body.xxx.return）
     */
    @Schema(description = "数据项KEY（响应取值路径）")
    private String dataItemKey;

    /** 是否分页接口：0 否，1 是；为 1 时作业同步按页拉取并分批写入 */
    @Schema(description = "是否分页接口：0否，1是")
    private Integer pageApi;

    /** 请求参数 JSON 原文（入库字段，含 headers/params/body/page/soapAction 等） */
    @Schema(description = "请求参数JSON")
    private String reqParamsJson;

    /** 非表字段：由 reqParamsJson 解析出的请求参数对象 */
    @TableField(exist = false)
    @Schema(description = "请求参数对象")
    private DsApiReqParams reqParams;

    /**
     * 兼容字段：单依赖时的 API 数据源主键（多依赖时同步为列表第一项）。
     * 序列化为字符串，避免前端雪花精度丢失。
     */
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "依赖API数据源ID（兼容旧单依赖；多依赖时取列表首项）")
    private Long relyDatasourceId;

    /**
     * 多依赖列表 JSON 原文（入库字段）。
     * 结构：[{ "alias":"auth", "datasourceId":"123", "sort":1 }, ...]
     */
    @Schema(description = "依赖API列表JSON")
    private String relyListJson;

    /** 非表字段：由 relyListJson 解析；编辑页直接提交此列表 */
    @TableField(exist = false)
    @Schema(description = "依赖API列表")
    private List<DsApiRelyItem> relyList;

    /** 是否依赖其它数据源：0 是（需配置 relyList / relyDatasourceId），1 否 */
    @Schema(description = "是否依赖数据源：0是，1否")
    private Integer belongToRelyDatasource;

    /** 数据源描述 */
    @Schema(description = "数据源描述")
    private String datasourceDesc;

    /** 状态：0 停用，1 启用 */
    @Schema(description = "状态：0停用，1启用")
    private Integer status;
}
