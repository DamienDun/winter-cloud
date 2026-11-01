package com.winter.cloud.common.dingtalk.clients;

import com.dingtalk.api.DefaultDingTalkClient;
import com.dingtalk.api.DingTalkClient;
import com.dingtalk.api.request.OapiSnsGetuserinfoBycodeRequest;
import com.dingtalk.api.request.OapiSsoGetuserinfoRequest;
import com.dingtalk.api.request.OapiV2UserGetuserinfoRequest;
import com.dingtalk.api.response.OapiSnsGetuserinfoBycodeResponse;
import com.dingtalk.api.response.OapiSsoGetuserinfoResponse;
import com.dingtalk.api.response.OapiV2UserGetuserinfoResponse;
import com.winter.cloud.common.core.exception.base.BaseException;
import com.winter.cloud.common.dingtalk.properties.WinterDingtalkProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * 钉钉身份认证(免密) 客户端
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2022/8/24 19:06
 */
@Slf4j
@Getter
@Setter
public class DtAuthClient extends AbstractDtClient implements IDtClient {

    public DtAuthClient(WinterDingtalkProperties properties) {
        super(properties);
    }

    /**
     * 通过免登码获取用户信息
     *
     * @param code
     * @return
     */
    public OapiV2UserGetuserinfoResponse.UserGetByCodeResponse getUserInfo(String code) {
        DingTalkClient client = new DefaultDingTalkClient(composeUrl("/topapi/v2/user/getuserinfo"));
        OapiV2UserGetuserinfoRequest req = new OapiV2UserGetuserinfoRequest();
        req.setCode(code);
        try {
            OapiV2UserGetuserinfoResponse rsp = client.execute(req, getAccessToken());
            log.info("ding getuserinfo rsp:{}", rsp.getBody());
            if (rsp.isSuccess()) {
                return rsp.getResult();
            } else {
                throw new BaseException(String.format("通过免登码获取用户信息失败:%s", rsp.getErrmsg()));
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new BaseException(String.format("通过免登码获取用户信息失败:%s", e.getMessage()));
        }
    }

    /**
     * 获取应用管理后台免登的用户信息
     *
     * @param code
     * @return
     */
    public OapiSsoGetuserinfoResponse getSsoUserInfo(String code) {
        DingTalkClient client = new DefaultDingTalkClient(composeUrl("/sso/getuserinfo"));
        OapiSsoGetuserinfoRequest req = new OapiSsoGetuserinfoRequest();
        req.setCode(code);
        req.setHttpMethod("GET");
        try {
            OapiSsoGetuserinfoResponse rsp = client.execute(req, getAccessToken());
            log.info("ding sso getuserinfo rsp:{}", rsp.getBody());
            if (rsp.isSuccess()) {
                return rsp;
            } else {
                throw new BaseException(String.format("获取应用管理后台免登的用户信息失败:%s", rsp.getErrmsg()));
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new BaseException(String.format("获取应用管理后台免登的用户信息失败:%s", e.getMessage()));
        }
    }

    /**
     * 根据sns临时授权码获取用户信息
     *
     * @param tmpAuthCode
     * @return
     */
    public OapiSnsGetuserinfoBycodeResponse.UserInfo getSnsUserInfo(String tmpAuthCode) {
        DefaultDingTalkClient client = new DefaultDingTalkClient(composeUrl("/sns/getuserinfo_bycode"));
        OapiSnsGetuserinfoBycodeRequest req = new OapiSnsGetuserinfoBycodeRequest();
        req.setTmpAuthCode(tmpAuthCode);
        try {
            OapiSnsGetuserinfoBycodeResponse rsp = client.execute(req, getProperties().getAppKey(), getProperties().getAppSecret());
            log.info("ding sns getuserinfo rsp:{}", rsp.getBody());
            if (rsp.isSuccess()) {
                return rsp.getUserInfo();
            } else {
                throw new BaseException(String.format("根据sns临时授权码获取用户信息失败:%s", rsp.getErrmsg()));
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new BaseException(String.format("根据sns临时授权码获取用户信息失败:%s", e.getMessage()));
        }
    }
}
