package com.winter.cloud.common.dingtalk.configure;

import com.winter.cloud.common.dingtalk.clients.IDtClient;
import com.winter.cloud.common.dingtalk.clients.*;
import com.winter.cloud.common.dingtalk.properties.WinterDingtalkProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * winter 钉钉自动装配
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2022/8/19 13:14
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({WinterDingtalkProperties.class})
public class WinterDingTalkAutoConfiguration {

    @Bean
    @ConditionalOnClass({IDtClient.class, AbstractDtClient.class})
    @ConditionalOnMissingBean(DtNoticeClient.class)
    public DtNoticeClient dtNoticeClient(WinterDingtalkProperties properties) {
        return new DtNoticeClient(properties);
    }

    @Bean
    @ConditionalOnClass({IDtClient.class, AbstractDtClient.class})
    @ConditionalOnMissingBean(DtChatClient.class)
    public DtChatClient dtChatClient(WinterDingtalkProperties properties) {
        return new DtChatClient(properties);
    }


    @Bean
    @ConditionalOnClass({IDtClient.class, AbstractDtClient.class})
    @ConditionalOnMissingBean(DtChatClient.class)
    public DtOrdinaryMsgClient dtOrdinaryMsgClient(WinterDingtalkProperties properties) {
        return new DtOrdinaryMsgClient(properties);
    }

    @Bean
    @ConditionalOnClass({IDtClient.class, AbstractDtClient.class})
    @ConditionalOnMissingBean(DtH5Client.class)
    public DtH5Client dtH5Client(WinterDingtalkProperties properties) {
        return new DtH5Client(properties);
    }

    @Bean
    @ConditionalOnClass({IDtClient.class, AbstractDtClient.class})
    @ConditionalOnMissingBean(DtAuthClient.class)
    public DtAuthClient dtAuthClient(WinterDingtalkProperties properties) {
        return new DtAuthClient(properties);
    }

    @Bean
    @ConditionalOnClass({IDtClient.class, AbstractDtClient.class})
    @ConditionalOnMissingBean(DtUserClient.class)
    public DtUserClient dtUserClient(WinterDingtalkProperties properties) {
        return new DtUserClient(properties);
    }
}
