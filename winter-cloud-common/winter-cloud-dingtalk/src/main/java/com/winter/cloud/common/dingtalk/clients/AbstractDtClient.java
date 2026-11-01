package com.winter.cloud.common.dingtalk.clients;

import com.aliyun.dingtalkoauth2_1_0.models.*;
import com.aliyun.teaopenapi.models.Config;
import com.aliyun.teautil.models.RuntimeOptions;
import com.dingtalk.api.DefaultDingTalkClient;
import com.dingtalk.api.DingTalkClient;
import com.dingtalk.api.request.OapiGetJsapiTicketRequest;
import com.dingtalk.api.request.OapiGettokenRequest;
import com.dingtalk.api.request.OapiServiceGetCorpTokenRequest;
import com.dingtalk.api.request.OapiSsoGettokenRequest;
import com.dingtalk.api.response.OapiGetJsapiTicketResponse;
import com.dingtalk.api.response.OapiGettokenResponse;
import com.dingtalk.api.response.OapiServiceGetCorpTokenResponse;
import com.dingtalk.api.response.OapiSsoGettokenResponse;
import com.taobao.api.ApiException;
import com.winter.cloud.common.core.config.WinterConfig;
import org.springframework.http.HttpMethod;
import com.winter.cloud.common.core.exception.ConfigureException;
import com.winter.cloud.common.core.exception.base.BaseException;
import com.winter.cloud.common.core.utils.ExceptionUtil;
import com.winter.cloud.common.dingtalk.support.LocalCacheUtil;
import com.winter.cloud.common.dingtalk.constants.DtConstant;
import com.winter.cloud.common.dingtalk.properties.WinterDingtalkProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import static com.winter.cloud.common.dingtalk.constants.DtAccessTokenTypeConstant.*;

/**
 * 抽象钉钉客户端
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2022/8/19 21:02
 */
@Slf4j
@Getter
@Setter
public abstract class AbstractDtClient implements IDtClient {

    /**
     * accessToken 缓存key
     */
    public final static String ACCESS_TOKEN = WinterDingtalkProperties.PREFIX + ".accesstoken";

    /**
     * jsapiTicket 缓存key
     */
    public final static String JSAPI_TICKET = WinterDingtalkProperties.PREFIX + ".jsapiticket";

    /**
     * 钉钉属性
     */
    private WinterDingtalkProperties properties;

    private com.aliyun.dingtalkoauth2_1_0.Client client;

    public AbstractDtClient(WinterDingtalkProperties properties) {
        this.properties = properties;
        try {
            client = new com.aliyun.dingtalkoauth2_1_0.Client(new Config()
                    .setProtocol(properties.getNewProtocol())
                    .setEndpoint(properties.getNewHost())
                    .setRegionId("central"));
        } catch (Exception e) {
            log.error("dingtalkoauth2_1_0 client create fail");
        }
    }

    /**
     * 根据钉钉配置属性获取凭证
     *
     * @return
     */
    @Override
    public String getAccessToken() {
        switch (properties.getAccessTokenType().toLowerCase()) {
            case INTERNAL_APP:
                return getInternalAppAccessToken();
            case CORP:
                return getCorpAccessToken();
            case SUITE:
                return getSuiteAccessToken();
            case SSO:
                return getSsoAccessToken();
            default:
                throw new ConfigureException(WinterConfig.PREFIX + ".dingtalk.accessTokenType 非法配置");
        }
    }

    /**
     * 创建jsapi ticket
     *
     * @return
     */
    @Override
    public String getJsapiTicket() {
        ExceptionUtil.checkNotNull(properties, "钉钉配置属性不能为空");
        Object cacheObj = LocalCacheUtil.get(JSAPI_TICKET);
        if (cacheObj != null) {
            return String.valueOf(cacheObj);
        }
        if (DtConstant.VERSION_OLD.equals(properties.getVersion())) {
            DingTalkClient client = new DefaultDingTalkClient(composeUrl("/get_jsapi_ticket"));
            OapiGetJsapiTicketRequest req = new OapiGetJsapiTicketRequest();
            req.setHttpMethod("GET");
            try {
                OapiGetJsapiTicketResponse body = client.execute(req, getAccessToken());
                LocalCacheUtil.set(JSAPI_TICKET, body.getTicket(), body.getExpiresIn() * 1000);
                return body.getTicket();
            } catch (ApiException e) {
                log.error(e.getMessage(), e);
                throw new BaseException("创建jsapi ticket失败");
            }
        }
        CreateJsapiTicketHeaders createJsapiTicketHeaders = new CreateJsapiTicketHeaders();
        createJsapiTicketHeaders.xAcsDingtalkAccessToken = getAccessToken();
        String jsapiTicket = null;
        try {
            CreateJsapiTicketResponse response = client.createJsapiTicketWithOptions(createJsapiTicketHeaders, new RuntimeOptions());
            jsapiTicket = response.getBody().getJsapiTicket();
            LocalCacheUtil.set(JSAPI_TICKET, jsapiTicket, response.getBody().getExpireIn() * 1000);
            return jsapiTicket;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new BaseException("创建jsapi ticket失败");
        }
    }

    /**
     * 获取企业内部应用的accessToken
     *
     * @return
     */
    public String getInternalAppAccessToken() {
        ExceptionUtil.checkNotNull(properties, "钉钉配置属性不能为空");
        ExceptionUtil.checkNotNullOrBlank(properties.getAppKey(), "dingtalk.appKey不能为空");
        ExceptionUtil.checkNotNullOrBlank(properties.getAppSecret(), "dingtalk.appSecret不能为空");
        String cacheKey = WinterDingtalkProperties.PREFIX + "." + INTERNAL_APP;
        Object cacheObj = LocalCacheUtil.get(cacheKey);
        if (cacheObj != null) {
            return String.valueOf(cacheObj);
        }
        if (DtConstant.VERSION_OLD.equals(properties.getVersion())) {
            DingTalkClient client = new DefaultDingTalkClient(composeUrl("/gettoken"));
            OapiGettokenRequest request = new OapiGettokenRequest();
            request.setAppkey(properties.getAppKey());
            request.setAppsecret(properties.getAppSecret());
            request.setHttpMethod(HttpMethod.GET.name());
            try {
                OapiGettokenResponse body = client.execute(request);
                LocalCacheUtil.set(cacheKey, body.getAccessToken(), body.getExpiresIn() * 1000);
                return body.getAccessToken();
            } catch (ApiException e) {
                log.error(e.getMessage(), e);
                throw new BaseException("获取企业内部应用的accessToken失败");
            }
        }
        GetAccessTokenRequest request = new GetAccessTokenRequest()
                .setAppKey(properties.getAppKey())
                .setAppSecret(properties.getAppSecret());
        try {
            GetAccessTokenResponseBody body = client.getAccessToken(request).getBody();
            LocalCacheUtil.set(cacheKey, body.getAccessToken(), body.getExpireIn() * 1000);
            return body.getAccessToken();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new BaseException("获取企业内部应用的accessToken失败");
        }
    }

    /**
     * 获取第三方应用授权企业的accessToken
     *
     * @return
     */
    public String getCorpAccessToken() {
        ExceptionUtil.checkNotNull(properties, "钉钉配置属性不能为空");
        ExceptionUtil.checkNotNullOrBlank(properties.getAppKey(), "dingtalk.appKey不能为空");
        ExceptionUtil.checkNotNullOrBlank(properties.getAppSecret(), "dingtalk.appSecret不能为空");
        ExceptionUtil.checkNotNullOrBlank(properties.getCorpId(), "dingtalk.corpId不能为空");
        ExceptionUtil.checkNotNullOrBlank(properties.getSuiteTicket(), "dingtalk.suiteTicket不能为空");
        String cacheKey = WinterDingtalkProperties.PREFIX + "." + CORP;
        Object cacheObj = LocalCacheUtil.get(cacheKey);
        if (cacheObj != null) {
            return String.valueOf(cacheObj);
        }
        if (DtConstant.VERSION_OLD.equals(properties.getVersion())) {
            DefaultDingTalkClient client = new DefaultDingTalkClient(composeUrl("/service/get_corp_token"));
            OapiServiceGetCorpTokenRequest req = new OapiServiceGetCorpTokenRequest();
            req.setAuthCorpid(properties.getCorpId());
            try {
                OapiServiceGetCorpTokenResponse body = client.execute(req, properties.getAppKey(), properties.getAppSecret(), properties.getSuiteTicket());
                LocalCacheUtil.set(cacheKey, body.getAccessToken(), body.getExpiresIn() * 1000);
                return body.getAccessToken();
            } catch (ApiException e) {
                log.error(e.getMessage(), e);
                throw new BaseException("获取第三方应用授权企业的accessToken失败");
            }
        }
        GetCorpAccessTokenRequest request = new GetCorpAccessTokenRequest()
                .setSuiteKey(this.properties.getAppKey())
                .setSuiteSecret(this.properties.getAppSecret())
                .setAuthCorpId(this.properties.getCorpId())
                .setSuiteTicket(this.properties.getSuiteTicket());
        try {
            GetCorpAccessTokenResponseBody body = client.getCorpAccessToken(request).getBody();
            LocalCacheUtil.set(cacheKey, body.getAccessToken(), body.getExpireIn() * 1000);
            return body.getAccessToken();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new BaseException("获取第三方应用授权企业的accessToken失败");
        }
    }

    /**
     * 获取第三方企业应用的suiteAccessToken
     *
     * @return
     */
    public String getSuiteAccessToken() {
        ExceptionUtil.checkNotNull(properties, "钉钉配置属性不能为空");
        ExceptionUtil.checkNotNullOrBlank(properties.getSuiteKey(), "suiteKey不能为空");
        ExceptionUtil.checkNotNullOrBlank(properties.getSuiteSecret(), "suiteSecret不能为空");
        ExceptionUtil.checkNotNullOrBlank(properties.getSuiteTicket(), "suiteTicket不能为空");
        String cacheKey = WinterDingtalkProperties.PREFIX + "." + SUITE;
        Object cacheObj = LocalCacheUtil.get(cacheKey);
        if (cacheObj != null) {
            return String.valueOf(cacheObj);
        }
        GetSuiteAccessTokenRequest getSuiteAccessTokenRequest = new GetSuiteAccessTokenRequest()
                .setSuiteKey(properties.getSuiteKey())
                .setSuiteSecret(properties.getSuiteSecret())
                .setSuiteTicket(properties.getSuiteTicket());
        try {
            GetSuiteAccessTokenResponseBody body = client.getSuiteAccessToken(getSuiteAccessTokenRequest).getBody();
            LocalCacheUtil.set(cacheKey, body.getAccessToken(), body.getExpireIn() * 1000);
            return body.getAccessToken();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new BaseException("获取第三方企业应用的suiteAccessToken失败");
        }
    }


    /**
     * 获取微应用后台免登的access_token
     *
     * @return
     */
    public String getSsoAccessToken() {
        ExceptionUtil.checkNotNull(properties, "钉钉配置属性不能为空");
        ExceptionUtil.checkNotNullOrBlank(properties.getCorpId(), "dingtalk.corpId不能为空");
        ExceptionUtil.checkNotNullOrBlank(properties.getSsoSecret(), "dingtalk.ssoSecret不能为空");
        String cacheKey = WinterDingtalkProperties.PREFIX + "." + SSO;
        Object cacheObj = LocalCacheUtil.get(cacheKey);
        if (cacheObj != null) {
            return String.valueOf(cacheObj);
        }
        if (DtConstant.VERSION_OLD.equals(properties.getVersion())) {
            DingTalkClient client = new DefaultDingTalkClient(composeUrl("/sso/gettoken"));
            OapiSsoGettokenRequest req = new OapiSsoGettokenRequest();
            req.setCorpid(properties.getCorpId());
            req.setCorpsecret(properties.getSsoSecret());
            req.setHttpMethod(HttpMethod.GET.name());
            try {
                OapiSsoGettokenResponse body = client.execute(req);
                LocalCacheUtil.set(cacheKey, body.getAccessToken(), 7200 * 1000);
                return body.getAccessToken();
            } catch (ApiException e) {
                log.error(e.getMessage(), e);
                throw new BaseException("获取微应用后台免登的access_token失败");
            }
        }
        GetSsoAccessTokenRequest request = new GetSsoAccessTokenRequest()
                .setCorpid(properties.getCorpId())
                .setSsoSecret(properties.getSsoSecret());
        try {
            GetSsoAccessTokenResponseBody body = client.getSsoAccessToken(request).getBody();
            LocalCacheUtil.set(cacheKey, body.getAccessToken(), body.getExpireIn() * 1000);
            return body.getAccessToken();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new BaseException("获取微应用后台免登的access_token失败");
        }
    }

    /**
     * 组成url
     *
     * @param pathname
     * @return
     */
    protected String composeUrl(String pathname) {
        return properties.getProtocol() + "://" + properties.getHost() + pathname;
    }
}
