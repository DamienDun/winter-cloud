package com.winter.cloud.datasource.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "API request params")
public class DsApiReqParams {
    private List<DsApiReqParamsItem> params;
    private List<DsApiReqParamsItem> headers;
    private String body;
    private List<DsApiReqParamsItem> page;
    private String preRequestScript;
}
