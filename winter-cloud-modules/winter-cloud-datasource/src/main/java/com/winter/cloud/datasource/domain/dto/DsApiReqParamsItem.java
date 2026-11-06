package com.winter.cloud.datasource.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "API request param item")
public class DsApiReqParamsItem {
    private String name;
    private String type;
    private String value;
    /** 0 available, 1 unavailable */
    private Integer isAvailable;
}
