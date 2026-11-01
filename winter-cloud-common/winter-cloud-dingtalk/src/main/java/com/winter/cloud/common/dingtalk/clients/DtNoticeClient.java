package com.winter.cloud.common.dingtalk.clients;

import com.dingtalk.api.DefaultDingTalkClient;
import com.dingtalk.api.DingTalkClient;
import com.dingtalk.api.request.*;
import com.dingtalk.api.response.*;
import com.winter.cloud.common.core.exception.base.BaseException;
import com.winter.cloud.common.dingtalk.constants.DtMsgTypeEnum;
import com.winter.cloud.common.dingtalk.properties.WinterDingtalkProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * 钉钉 消息通知-工作通知客户端
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2022/8/19 19:19
 */
@Slf4j
@Getter
@Setter
public class DtNoticeClient extends AbstractDtClient implements IDtClient {

    public DtNoticeClient(WinterDingtalkProperties properties) {
        super(properties);
    }

    /**
     * 指定用户 发送工作通知
     *
     * @param msg        消息
     * @param useridList 用户id集合(,分隔)
     * @return taskId    创建的异步发送任务ID
     */
    public Long send(OapiMessageCorpconversationAsyncsendV2Request.Msg msg, String useridList) {
        DingTalkClient client = new DefaultDingTalkClient(composeUrl("/topapi/message/corpconversation/asyncsend_v2"));
        OapiMessageCorpconversationAsyncsendV2Request request = new OapiMessageCorpconversationAsyncsendV2Request();
        request.setAgentId(getProperties().getAgentId());
        request.setUseridList(useridList);
        request.setToAllUser(false);
        request.setMsg(msg);
        try {
            OapiMessageCorpconversationAsyncsendV2Response rsp = client.execute(request, getAccessToken());
            log.info("ding notice sendMsg rsp:{}", rsp.getBody());
            if (rsp.isSuccess()) {
                return rsp.getTaskId();
            } else {
                throw new BaseException(String.format("钉钉发送工作通知失败:%s", rsp.getErrmsg()));
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new BaseException(String.format("钉钉发送工作通知失败:%s", e.getMessage()));
        }
    }

    /**
     * 发送文本类的钉钉工作通知
     *
     * @param content    消息内容
     * @param useridList 用户id集合(,分隔)
     * @return
     */
    public Long sendTextMsg(String content, String useridList) {
        OapiMessageCorpconversationAsyncsendV2Request.Msg msg = new OapiMessageCorpconversationAsyncsendV2Request.Msg();
        msg.setMsgtype(DtMsgTypeEnum.TEXT.getName());
        msg.setText(new OapiMessageCorpconversationAsyncsendV2Request.Text());
        msg.getText().setContent(content);
        return send(msg, useridList);
    }

    /**
     * 发送卡片类的钉钉工作通知
     *
     * @param actionCard 卡片内容
     * @param useridList 用户id集合(,分隔)
     * @return
     */
    public Long sendActionCardMsg(OapiMessageCorpconversationAsyncsendV2Request.ActionCard actionCard, String useridList) {
        OapiMessageCorpconversationAsyncsendV2Request.Msg msg = new OapiMessageCorpconversationAsyncsendV2Request.Msg();
        msg.setMsgtype(DtMsgTypeEnum.ACTION_CARD.getName());
        msg.setActionCard(actionCard);
        return send(msg, useridList);
    }

    /**
     * 获取工作通知消息的发送结果
     *
     * @param taskId 创建的异步发送任务id
     * @return
     */
    public OapiMessageCorpconversationGetsendresultResponse sendMsgResultRsp(Long taskId) {
        DingTalkClient client = new DefaultDingTalkClient(composeUrl("/topapi/message/corpconversation/getsendresult"));
        OapiMessageCorpconversationGetsendresultRequest req = new OapiMessageCorpconversationGetsendresultRequest();
        req.setAgentId(getProperties().getAgentId());
        req.setTaskId(taskId);
        try {
            OapiMessageCorpconversationGetsendresultResponse rsp = client.execute(req, getAccessToken());
            log.info("ding notice sendMsgResult rsp:{}", rsp.getBody());
            if (rsp.isSuccess()) {
                return rsp;
            } else {
                throw new BaseException(String.format("获取工作通知消息的发送结果失败:%s", rsp.getErrmsg()));
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new BaseException(String.format("获取工作通知消息的发送结果失败:%s", e.getMessage()));
        }
    }

    /**
     * 更新工作通知状态栏
     *
     * @param taskId      工作通知任务ID 企业内部应用调用发送工作通知接口获取。钉钉三方企业应用调用发送工作通知接口获取。
     * @param statusValue 状态栏值
     */
    public void updateStatus(Long taskId, String statusValue) {
        this.updateStatus(taskId, statusValue, "0xFF78C06E");
    }

    /**
     * 更新工作通知状态栏
     *
     * @param taskId      工作通知任务ID 企业内部应用调用发送工作通知接口获取。钉钉三方企业应用调用发送工作通知接口获取。
     * @param statusValue 状态栏值
     * @param statusBg    状态栏背景色，推荐0xFF加六位颜色值
     */
    public void updateStatus(Long taskId, String statusValue, String statusBg) {
        DingTalkClient client = new DefaultDingTalkClient(composeUrl("/topapi/message/corpconversation/status_bar/update"));
        OapiMessageCorpconversationStatusBarUpdateRequest request = new OapiMessageCorpconversationStatusBarUpdateRequest();
        request.setAgentId(getProperties().getAgentId());
        request.setStatusValue(statusValue);
        request.setStatusBg(statusBg);
        request.setTaskId(taskId);
        OapiMessageCorpconversationStatusBarUpdateResponse rsp;
        try {
            rsp = client.execute(request, getAccessToken());
            log.info("ding notice updatestatus rsp:{}", rsp.getBody());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new BaseException(String.format("更新工作通知状态栏失败:%s", e.getMessage()));
        }
        if (!rsp.isSuccess()) {
            throw new BaseException(String.format("更新工作通知状态栏失败:%s", rsp.getMessage()));
        }
    }

    /**
     * 获取工作通知消息的发送进度
     *
     * @param taskId 工作通知任务ID
     * @return
     */
    public OapiMessageCorpconversationGetsendprogressResponse getsendprogress(Long taskId) {
        DingTalkClient client = new DefaultDingTalkClient(composeUrl("/topapi/message/corpconversation/getsendprogress"));
        OapiMessageCorpconversationGetsendprogressRequest req = new OapiMessageCorpconversationGetsendprogressRequest();
        req.setAgentId(getProperties().getAgentId());
        req.setTaskId(taskId);
        try {
            OapiMessageCorpconversationGetsendprogressResponse rsp = client.execute(req, getAccessToken());
            log.info("ding notice getsendprogress rsp:{}", rsp.getBody());
            if (rsp.isSuccess()) {
                return rsp;
            } else {
                throw new BaseException(String.format("获取工作通知消息的发送进度失败:%s", rsp.getErrmsg()));
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new BaseException(String.format("获取工作通知消息的发送进度失败:%s", e.getMessage()));
        }
    }

    /**
     * 获取工作通知消息的发送结果
     *
     * @param taskId 工作通知任务ID
     * @return
     */
    public OapiMessageCorpconversationGetsendresultResponse getsendresult(Long taskId) {
        DingTalkClient client = new DefaultDingTalkClient(composeUrl("/topapi/message/corpconversation/getsendresult"));
        OapiMessageCorpconversationGetsendresultRequest req = new OapiMessageCorpconversationGetsendresultRequest();
        req.setAgentId(getProperties().getAgentId());
        req.setTaskId(taskId);
        try {
            OapiMessageCorpconversationGetsendresultResponse rsp = client.execute(req, getAccessToken());
            log.info("ding notice getsendresult rsp:{}", rsp.getBody());
            if (rsp.isSuccess()) {
                return rsp;
            } else {
                throw new BaseException(String.format("获取工作通知消息的发送进度失败:%s", rsp.getErrmsg()));
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new BaseException(String.format("获取工作通知消息的发送进度失败:%s", e.getMessage()));
        }
    }

    /**
     * 撤回工作通知消息
     *
     * @param taskId 工作通知任务ID
     */
    public void recall(Long taskId) {
        DingTalkClient client = new DefaultDingTalkClient(composeUrl("/topapi/message/corpconversation/recall"));
        OapiMessageCorpconversationRecallRequest req = new OapiMessageCorpconversationRecallRequest();
        req.setAgentId(getProperties().getAgentId());
        req.setMsgTaskId(taskId);
        OapiMessageCorpconversationRecallResponse rsp;
        try {
            rsp = client.execute(req, getAccessToken());
            log.info("ding notice recall rsp:{}", rsp.getBody());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new BaseException(String.format("撤回工作通知消息失败:%s", e.getMessage()));
        }
        if (!rsp.isSuccess()) {
            throw new BaseException(String.format("撤回工作通知消息失败:%s", rsp.getErrmsg()));
        }
    }
}
