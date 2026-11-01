package com.winter.cloud.common.dingtalk.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotBlank;

import java.io.Serializable;

/**
 * 钉钉身份认证(免密)登录入参
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2022/8/25 16:20
 */
@Getter
@Setter
@NoArgsConstructor
@Schema(description = "钉钉身份认证(免密)登录入参")
public class DtAuthLoginInputDto implements Serializable {

    private static final long serialVersionUID = 8657811157025047520L;

    @NotBlank(message = "code不能为空")
    @Schema(description = "code")
    private String code;
}
