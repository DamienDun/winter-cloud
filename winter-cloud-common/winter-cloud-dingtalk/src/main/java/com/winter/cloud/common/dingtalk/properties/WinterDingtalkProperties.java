package com.winter.cloud.common.dingtalk.properties;

import com.winter.cloud.common.core.config.WinterConfig;
import com.winter.cloud.common.dingtalk.constants.DtAccessTokenTypeConstant;
import com.winter.cloud.common.dingtalk.constants.DtConstant;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.io.Serializable;

/**
 * winter 钉钉属性
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2022/8/19 13:15
 */
@ConfigurationProperties(prefix = WinterDingtalkProperties.PREFIX)
@ToString(callSuper = true)
@Getter
@Setter
public class WinterDingtalkProperties implements Serializable {

    private static final long serialVersionUID = 3890994637802171585L;

    /**
     * 属性前缀
     */
    public final static String PREFIX = WinterConfig.PREFIX + ".dingtalk";

    /**
     * bean条件属性
     */
    public static final String BEAN_CONDITIONAL_PROPERTY = WinterDingtalkProperties.PREFIX + ".accessTokenMode";

    /**
     * 获取凭证类型
     */
    private String accessTokenType = DtAccessTokenTypeConstant.INTERNAL_APP;

    /**
     * 授权企业的CorpId
     */
    private String corpId;

    /**
     * 应用id
     */
    private Long agentId;

    /**
     * appKey,定制应用则对应CustomKey,第三方则对应SuiteKey
     * <p>
     * 如果是定制应用，输入定制应用的CustomKey，可在开发者后台的应用详情页获取。
     * 如果是第三方企业应用，输入第三方企业应用的SuiteKey，可在开发者后台的应用详情页获取。
     */
    private String appKey;

    /**
     * appSecret
     * <p>
     * 如果是定制应用，输入定制应用的CustomSecret，可在开发者后台的应用详情页获取。
     * 如果是第三方企业应用，输入第三方企业应用的SuiteSecret，可在开发者后台的应用详情页获取。
     */
    private String appSecret;

    /**
     * 已创建的第三方企业应用的SuiteKey
     */
    private String suiteKey;

    /**
     * 已创建的第三方企业应用的SuiteSecret
     */
    private String suiteSecret;

    /**
     * 钉钉推送的suiteTicket。
     * <p>
     * 定制应用可随意填写。
     * 第三方企业应用使用钉钉开放平台向应用推送的suite_ticket，请参考数据格式biz_type=2
     */
    private String suiteTicket;

    /**
     * sso密钥，可以在开发者后台基本信息—开发信息（旧版）页面查看。
     */
    private String ssoSecret;

    /**
     * 旧版api调用的服务地址
     */
    private String host = DtConstant.OLD_HOST;

    /**
     * 旧版api协议
     */
    private String protocol = DtConstant.HTTPS_PROTOCOL;

    /**
     * 新版api调用的服务地址
     */
    private String newHost = DtConstant.NEW_HOST;

    /**
     * 新版api协议
     */
    private String newProtocol = DtConstant.HTTPS_PROTOCOL;

    /**
     * 版本
     */
    private String version = DtConstant.VERSION_OLD;

    public WinterDingtalkProperties() {
    }
}
