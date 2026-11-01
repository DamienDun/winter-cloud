package com.winter.cloud.common.dingtalk.constants;

/**
 * 应用身份访问凭证常量类
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2022/8/19 16:14
 */
public interface DtAccessTokenTypeConstant {

    String USER = "user";

    /**
     * 获取企业内部应用的access_token
     */
    String INTERNAL_APP = "internalapp";

    /**
     * 获取第三方应用授权企业的accessToken
     */
    String CORP = "corp";

    /**
     * 获取第三方企业应用的suiteAccessToken
     */
    String SUITE = "suite";

    /**
     * 获取微应用后台免登的access_token
     */
    String SSO = "sso";
}
