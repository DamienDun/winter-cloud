package com.winter.cloud.common.web.configure;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2022/12/16 14:13
 */
public class WinterWebAutoConfiguration implements WebMvcConfigurer {

    @Bean
    @ConditionalOnMissingBean(WinterWebBeanPostProcessor.class)
    public BeanPostProcessor cellWebBeanPostProcessor() {
        return new WinterWebBeanPostProcessor();
    }
}
