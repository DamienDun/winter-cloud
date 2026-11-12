package com.winter.cloud.datasource.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * API 请求参数聚合（序列化进 req_params_json）
 */
@Data
@Schema(description = "API请求参数")
public class DsApiReqParams {

    /** URL 查询参数列表 */
    @Schema(description = "查询参数列表")
    private List<DsApiReqParamsItem> params;

    /** HTTP 请求头列表 */
    @Schema(description = "请求头列表")
    private List<DsApiReqParamsItem> headers;

    /** 请求体：HTTP 一般为 JSON；WebService 为 SOAP XML */
    @Schema(description = "请求体（JSON或SOAP XML）")
    private String body;

    /** 分页相关参数列表（pageNum/pageSize 等，可与 params 并存） */
    @Schema(description = "分页参数列表")
    private List<DsApiReqParamsItem> page;

    /** 前置脚本（预留，当前可空） */
    @Schema(description = "前置脚本（预留）")
    private String preRequestScript;

    /** WebService 的 SOAPAction 头值（可空；仅 datasourceType=WEBSERVICE 使用） */
    @Schema(description = "SOAPAction（仅WebService）")
    private String soapAction;
}
