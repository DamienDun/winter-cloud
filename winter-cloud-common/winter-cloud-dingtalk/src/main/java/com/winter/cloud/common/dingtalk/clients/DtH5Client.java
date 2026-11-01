package com.winter.cloud.common.dingtalk.clients;

import com.winter.cloud.common.dingtalk.properties.WinterDingtalkProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.http.HttpServletRequest;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 钉钉H5 客户端
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2022/8/19 20:58
 */
@Slf4j
@Getter
@Setter
public class DtH5Client extends AbstractDtClient implements IDtClient {

    public DtH5Client(WinterDingtalkProperties properties) {
        super(properties);
    }

    /**
     * 页面请求服务端获取鉴权参数
     *
     * @param request
     * @return
     */
    public Map<String, Object> getConfig(HttpServletRequest request) {
        //获取当前页面的完整URL路径，用于获取签名signature
        String urlString = request.getRequestURL().toString();
        String queryString = request.getQueryString();
        String queryStringEncode = null;
        String url;
        if (queryString != null) {
            queryStringEncode = URLDecoder.decode(queryString);
            url = urlString + "?" + queryStringEncode;
        } else {
            url = urlString;
        }
        //随机字符串，用于获取签名signature，并返回给页面
        String nonceStr = UUID.randomUUID().toString().replaceAll("-", "");
        //时间戳，用于获取签名signature，并返回给页面
        long timeStamp = System.currentTimeMillis() / 1000;
        String signedUrl = url;
        String ticket = getJsapiTicket();
        String signature = sign(ticket, nonceStr, timeStamp, signedUrl);
        //封装返回给页面的信息
        Map<String, Object> configValue = new HashMap<>();
        configValue.put("jsticket", ticket);
        configValue.put("signature", signature);
        configValue.put("nonceStr", nonceStr);
        configValue.put("timeStamp", timeStamp);
        configValue.put("corpId", getProperties().getCorpId());
        configValue.put("agentId", getProperties().getAgentId());
        return configValue;
    }

    /**
     * 获取签名signature
     *
     * @param ticket
     * @param nonceStr
     * @param timeStamp
     * @param url
     * @return
     */
    private String sign(String ticket, String nonceStr, long timeStamp, String url) {
        String plainTex = "jsapi_ticket=" + ticket + "&noncestr=" + nonceStr + "&timestamp=" + timeStamp + "&url=" + url;
        String signature = "";
        try {
            MessageDigest crypt = MessageDigest.getInstance("SHA-256");
            crypt.reset();
            crypt.update(plainTex.getBytes(StandardCharsets.UTF_8));
            signature = byteToHex(crypt.digest());
            return signature;
        } catch (Exception e) {
            log.error(String.format("生成钉钉H5 JsapiTicket签名失败:%s", e.getMessage()));
        }
        return signature;
    }

    private String byteToHex(byte[] hash) {
        Formatter formatter = new Formatter();
        byte[] var2 = hash;
        int var3 = hash.length;
        for (int var4 = 0; var4 < var3; ++var4) {
            byte b = var2[var4];
            formatter.format("%02x", b);
        }
        String result = formatter.toString();
        formatter.close();
        return result;
    }
}
