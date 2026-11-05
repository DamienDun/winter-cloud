package com.winter.cloud.datasource.domain.dto;

import com.winter.cloud.common.datasource.ddl.model.DsColumnDef;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 建表请求
 */
@Getter
@Setter
@NoArgsConstructor
@Schema(description = "建表请求")
public class DsCreateTableRequest {

    @NotNull
    private Long datasourceId;

    @NotBlank
    private String tableName;

    private String comment;

    @NotEmpty
    private List<DsColumnDef> columns;
}