package com.winter.cloud.common.dingtalk.factory;

/**
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2022/8/19 16:18
 */
public class AccessTokenFactory {

//    /**
//     * 根据钉钉配置属性获取凭证
//     *
//     * @param properties dingtalk配置属性
//     * @return
//     */
//    public static String getAccessToken(WinterDingtalkProperties properties) {
//        ExceptionUtil.checkNotNullOrBlank(properties.getAccessTokenMode(), "凭证模式不能为空");
//        String accessToken = String.valueOf(LocalCacheUtil.get(WinterDingtalkProperties.ACCESS_TOKEN));
//        if (StringUtils.isNotEmpty(accessToken)) {
//            return accessToken;
//        }
//        AccessTokenResp accessTokenResp;
//        switch (properties.getAccessTokenMode().toLowerCase()) {
//            case ACCESS_TOKEN:
//                accessTokenResp = AccessTokenClient.getAccessToken(properties);
//                break;
//            case CORP_ACCESS_TOKEN:
//                accessTokenResp = AccessTokenClient.getCorpAccessToken(properties);
//                break;
//            case SSO:
//                accessTokenResp = AccessTokenClient.getSsoAccessToken(properties);
//                break;
//            default:
//                throw new ConfigureException(WinterConfig.ACCESS_TOKEN + ".dingtalk.accessTokenMode 非法配置");
//        }
//        LocalCacheUtil.set(WinterDingtalkProperties.ACCESS_TOKEN, accessTokenResp.getAccessToken(), accessTokenResp.getExpireIn() * 1000);
//        return accessTokenResp.getAccessToken();
//    }
}
