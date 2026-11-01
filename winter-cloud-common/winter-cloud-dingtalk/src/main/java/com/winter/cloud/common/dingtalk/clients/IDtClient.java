package com.winter.cloud.common.dingtalk.clients;

/**
 * 钉钉客户端
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2022/8/19 20:59
 */
public interface IDtClient {

    /**
     * 获取钉钉凭证
     *
     * @return
     */
    String getAccessToken();

    /**
     * 获取jsapiTicket
     *
     * @return
     */
    String getJsapiTicket();
}
