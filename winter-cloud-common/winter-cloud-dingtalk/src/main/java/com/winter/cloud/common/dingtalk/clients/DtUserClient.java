package com.winter.cloud.common.dingtalk.clients;

import com.dingtalk.api.DefaultDingTalkClient;
import com.dingtalk.api.DingTalkClient;
import com.dingtalk.api.request.OapiUserGetbyunionidRequest;
import com.dingtalk.api.request.OapiUserListidRequest;
import com.dingtalk.api.request.OapiV2UserGetRequest;
import com.dingtalk.api.request.OapiV2UserGetbymobileRequest;
import com.dingtalk.api.response.OapiUserGetbyunionidResponse;
import com.dingtalk.api.response.OapiUserListidResponse;
import com.dingtalk.api.response.OapiV2UserGetResponse;
import com.dingtalk.api.response.OapiV2UserGetbymobileResponse;
import com.winter.cloud.common.core.exception.base.BaseException;
import com.winter.cloud.common.dingtalk.properties.WinterDingtalkProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 钉钉用户管理客户端
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2022/8/25 16:47
 */
@Slf4j
@Getter
@Setter
public class DtUserClient extends AbstractDtClient implements IDtClient {

    public DtUserClient(WinterDingtalkProperties properties) {
        super(properties);
    }

    /**
     * 根据用户id获取用户信息
     *
     * @param userId 钉钉用户id
     * @return
     */
    public OapiV2UserGetResponse.UserGetResponse get(String userId) {
        try {
            DingTalkClient client = new DefaultDingTalkClient(composeUrl("/topapi/v2/user/get"));
            OapiV2UserGetRequest req = new OapiV2UserGetRequest();
            req.setUserid(userId);
            req.setLanguage("zh_CN");
            OapiV2UserGetResponse rsp = client.execute(req, getAccessToken());
            log.info("ding get userinfo rsp:{}", rsp.getBody());
            if (rsp.isSuccess()) {
                return rsp.getResult();
            } else {
                throw new BaseException(String.format("ding get userinfo fail:%s", rsp.getErrmsg()));
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new BaseException(String.format("ding get userinfo fail:%s", e.getMessage()));
        }
    }

    /**
     * 根据手机号获取钉钉用户id
     *
     * @param mobile 手机号
     * @return
     */
    public String getByMobile(String mobile) {
        try {
            DingTalkClient client = new DefaultDingTalkClient(composeUrl("/topapi/v2/user/getbymobile"));
            OapiV2UserGetbymobileRequest req = new OapiV2UserGetbymobileRequest();
            req.setMobile(mobile);
            OapiV2UserGetbymobileResponse rsp = client.execute(req, getAccessToken());
            log.info("ding userid getbymobile rsp:{}", rsp.getBody());
            if (rsp.isSuccess()) {
                return rsp.getResult().getUserid();
            } else {
                throw new BaseException(String.format("ding get userid by mobile fail:%s", rsp.getErrmsg()));
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new BaseException(String.format("ding get userid by mobile fail:%s", e.getMessage()));
        }
    }

    /**
     * 根据unionid获取钉钉用户id
     *
     * @param unionid
     * @return
     */
    public String getByUnionid(String unionid) {
        DingTalkClient client = new DefaultDingTalkClient(composeUrl("/topapi/user/getbyunionid"));
        OapiUserGetbyunionidRequest req = new OapiUserGetbyunionidRequest();
        req.setUnionid(unionid);
        try {
            OapiUserGetbyunionidResponse rsp = client.execute(req, getAccessToken());
            log.info("ding userid getbyunionid rsp:{}", rsp.getBody());
            if (rsp.isSuccess()) {
                return rsp.getResult().getUserid();
            } else {
                throw new BaseException(String.format("ding get userid by unionid fail:%s", rsp.getErrmsg()));
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new BaseException(String.format("ding get userid by unionid fail:%s", e.getMessage()));
        }
    }

    /**
     * 根据部门id获取用户id列表
     *
     * @param deptId 钉钉部门id
     * @return
     */
    public List<String> listByDeptId(Long deptId) {
        DingTalkClient client = new DefaultDingTalkClient(composeUrl("/topapi/user/listid"));
        OapiUserListidRequest req = new OapiUserListidRequest();
        req.setDeptId(deptId);

        try {
            OapiUserListidResponse rsp = client.execute(req, getAccessToken());
            log.info("ding userid listbydeptid rsp:{}", rsp.getBody());
            if (rsp.isSuccess()) {
                return rsp.getResult().getUseridList();
            } else {
                throw new BaseException(String.format("ding userid listbydeptid fail:%s", rsp.getErrmsg()));
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new BaseException(String.format("ding userid listbydeptid fail:%s", e.getMessage()));
        }
    }
}
