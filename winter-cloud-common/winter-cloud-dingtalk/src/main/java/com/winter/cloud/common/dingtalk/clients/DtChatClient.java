package com.winter.cloud.common.dingtalk.clients;

import com.dingtalk.api.DefaultDingTalkClient;
import com.dingtalk.api.DingTalkClient;
import com.dingtalk.api.request.OapiChatGetReadListRequest;
import com.dingtalk.api.request.OapiChatSendRequest;
import com.dingtalk.api.response.OapiChatGetReadListResponse;
import com.dingtalk.api.response.OapiChatSendResponse;
import com.winter.cloud.common.core.exception.base.BaseException;
import com.winter.cloud.common.dingtalk.constants.DtMsgTypeEnum;
import com.winter.cloud.common.dingtalk.properties.WinterDingtalkProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * 钉钉 消息通知-企业群消息客户端
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2022/8/26 10:48
 */
@Slf4j
@Getter
@Setter
public class DtChatClient extends AbstractDtClient implements IDtClient {

    public DtChatClient(WinterDingtalkProperties properties) {
        super(properties);
    }

    /**
     * 发送消息到企业群
     *
     * @param msg    消息
     * @param chatId 群会话id
     */
    public String send(OapiChatSendRequest.Msg msg, String chatId) {
        DingTalkClient client = new DefaultDingTalkClient(composeUrl("/chat/send"));
        OapiChatSendRequest req = new OapiChatSendRequest();
        req.setMsg(msg);
        req.setChatid(chatId);
        OapiChatSendResponse rsp = null;
        try {
            rsp = client.execute(req, getAccessToken());
            log.info("ding chat sendMsg rsp:{}", rsp.getBody());
            if (rsp.isSuccess()) {
                return rsp.getMessageId();
            } else {
                throw new BaseException(String.format("发送消息到企业群失败,chatId:{%s}:%s", chatId, rsp.getErrmsg()));
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new BaseException(String.format("发送消息到企业群失败,chatId:{%s}:%s", chatId, e.getMessage()));
        }
    }

    /**
     * 发送TEXT类型消息到企业群
     *
     * @param content 内容
     * @param chatId  群会话id
     * @return messageId 加密消息id
     */
    public String sendText(String content, String chatId) {
        OapiChatSendRequest.Msg msg = new OapiChatSendRequest.Msg();
        OapiChatSendRequest.Text text = new OapiChatSendRequest.Text();
        text.setContent(content);
        msg.setText(text);
        msg.setMsgtype(DtMsgTypeEnum.TEXT.getName());
        return this.send(msg, chatId);
    }

    /**
     * 查询群消息已读人员列表
     *
     * @param messageId 发送消息到企业群接口返回的加密消息id
     * @param cursor    分页查询的游标，第一次可以传0，后续传返回结果中的next_cursor的值。
     *                  返回结果中，没有next_cursor时，表示没有后续的数据了，可以结束调用。
     * @param size      分页查询的大小，最大可以传100，且不能超过群的总人数
     * @return
     */
    public OapiChatGetReadListResponse getReadList(String messageId, Long cursor, Long size) {
        DingTalkClient client = new DefaultDingTalkClient(composeUrl("/chat/getReadList"));
        OapiChatGetReadListRequest req = new OapiChatGetReadListRequest();
        req.setMessageId(messageId);
        req.setCursor(cursor);
        req.setSize(size);
        req.setHttpMethod("GET");
        OapiChatGetReadListResponse rsp = null;
        try {
            rsp = client.execute(req, getAccessToken());
            log.info("ding chat getReadList rsp:{}", rsp.getBody());
            if (rsp.isSuccess()) {
                return rsp;
            } else {
                throw new BaseException(String.format("查询群消息已读人员列表失败,messageId:{%s},%s", messageId, rsp.getErrmsg()));
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new BaseException(String.format("查询群消息已读人员列表失败,messageId:{%s},%s", messageId, e.getMessage()));
        }
    }
}
