package com.winter.cloud.auth.controller;

import com.winter.cloud.auth.form.LoginBody;
import com.winter.cloud.auth.form.RegisterBody;
import com.winter.cloud.auth.service.SysLoginService;
import com.winter.cloud.common.core.domain.R;
import com.winter.cloud.common.core.utils.JwtUtils;
import com.winter.cloud.common.core.utils.StringUtils;
import com.winter.cloud.common.security.auth.AuthUtil;
import com.winter.cloud.common.security.service.TokenService;
import com.winter.cloud.common.security.utils.SecurityUtils;
import com.winter.cloud.system.api.model.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * token 控制
 *
 * @author winter
 */
@Tag(name = "认证授权")
@RestController
public class TokenController {
    @Autowired
    private TokenService tokenService;

    @Autowired
    private SysLoginService sysLoginService;

    @Operation(summary = "用户登录")
    @PostMapping("login")
    public R<?> login(@RequestBody LoginBody form) {
        // 用户登录
        LoginUser userInfo = sysLoginService.login(form.getUsername(), form.getPassword());
        // 获取登录token
        return R.ok(tokenService.createToken(userInfo));
    }

    @Operation(summary = "用户退出")
    @DeleteMapping("logout")
    public R<?> logout(HttpServletRequest request) {
        String token = SecurityUtils.getToken(request);
        if (StringUtils.isNotEmpty(token)) {
            String username = JwtUtils.getUserName(token);
            // 删除用户缓存记录
            AuthUtil.logoutByToken(token);
            // 记录用户退出日志
            sysLoginService.logout(username);
        }
        return R.ok();
    }

    @Operation(summary = "刷新令牌")
    @PostMapping("refresh")
    public R<?> refresh(HttpServletRequest request) {
        LoginUser loginUser = tokenService.getLoginUser(request);
        if (StringUtils.isNotNull(loginUser)) {
            // 刷新令牌有效期
            tokenService.refreshToken(loginUser);
            return R.ok();
        }
        return R.ok();
    }

    @Operation(summary = "用户注册")
    @PostMapping("register")
    public R<?> register(@RequestBody RegisterBody registerBody) {
        // 用户注册
        sysLoginService.register(registerBody.getUsername(), registerBody.getPassword());
        return R.ok();
    }
}
