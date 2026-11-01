package com.winter.cloud.common.dingtalk.clients;

import com.dingtalk.api.DefaultDingTalkClient;
import com.dingtalk.api.DingTalkClient;
import com.dingtalk.api.request.OapiMessageSendToConversationRequest;
import com.dingtalk.api.response.OapiMessageSendToConversationResponse;
import com.winter.cloud.common.core.exception.base.BaseException;
import com.winter.cloud.common.dingtalk.properties.WinterDingtalkProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * 钉钉 消息通知-普通消息客户端
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2022/8/26 11:11
 */
@Slf4j
@Getter
@Setter
public class DtOrdinaryMsgClient extends AbstractDtClient implements IDtClient {

    public DtOrdinaryMsgClient(WinterDingtalkProperties properties) {
        super(properties);
    }

    /**
     * 发送普通消息
     *
     * @param msg    消息
     * @param sender 消息发送者的userid
     * @param cid    群会话或者个人会话的id
     * @return
     */
    public String send(OapiMessageSendToConversationRequest.Msg msg, String sender, String cid) {
        DingTalkClient client = new DefaultDingTalkClient(composeUrl("/message/send_to_conversation"));
        OapiMessageSendToConversationRequest req = new OapiMessageSendToConversationRequest();
        req.setSender(sender);
        req.setCid(cid);
        req.setMsg(msg);
        OapiMessageSendToConversationResponse rsp = null;
        try {
            rsp = client.execute(req, getAccessToken());
            log.info("ding commonmsg sendMsg rsp:{}", rsp.getBody());
            if (rsp.isSuccess()) {
                return rsp.getReceiver();
            } else {
                throw new BaseException(String.format("发送普通消息失败:%s", rsp.getErrmsg()));
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new BaseException(String.format("发送普通消息失败:%s", e.getMessage()));
        }
    }
}
