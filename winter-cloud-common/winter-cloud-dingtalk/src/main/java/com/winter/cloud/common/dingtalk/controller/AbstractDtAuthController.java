package com.winter.cloud.common.dingtalk.controller;

import com.dingtalk.api.response.OapiSsoGetuserinfoResponse;
import com.dingtalk.api.response.OapiV2UserGetuserinfoResponse;
import com.winter.cloud.common.core.web.domain.AjaxResult;
import com.winter.cloud.common.dingtalk.clients.DtAuthClient;
import com.winter.cloud.common.dingtalk.constants.DtUrlConstant;
import com.winter.cloud.common.dingtalk.dto.DtAuthLoginInputDto;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 抽象 钉钉身份认证接口
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2022/8/25 10:40
 */
@RequestMapping(DtUrlConstant.PREFIX + "/auth")
@Tag(name = "钉钉身份验证(免登)")
public abstract class AbstractDtAuthController {

    @Autowired
    private DtAuthClient dtAuthUserClient;

    /**
     * 欢迎页面，通过 /welcome 访问，判断后端服务是否启动
     *
     * @return 字符串 welcome
     */
    @GetMapping("/welcome")
    public String welcome() {
        return "welcome";
    }

    @PostMapping("/login")
    @Operation(summary = "企业内部应用免登")
    public AjaxResult login(@RequestBody @Valid DtAuthLoginInputDto input) {
        OapiV2UserGetuserinfoResponse.UserGetByCodeResponse userInfo = dtAuthUserClient.getUserInfo(input.getCode());
        AjaxResult result = AjaxResult.success();
        String token = createToken(userInfo.getUserid());
        result.put("token", token);
        return result;
    }

    protected abstract String createToken(String dingUserId);

    @PostMapping("/admin/login")
    @Operation(summary = "应用管理后台免登")
    public AjaxResult getAdminInfo(@RequestBody @Valid DtAuthLoginInputDto input) {
        OapiSsoGetuserinfoResponse resp = dtAuthUserClient.getSsoUserInfo(input.getCode());
        AjaxResult result = AjaxResult.success();
        String token = createToken(resp.getUserInfo().getUserid());
        result.put("token", token);
        return result;
    }

}
