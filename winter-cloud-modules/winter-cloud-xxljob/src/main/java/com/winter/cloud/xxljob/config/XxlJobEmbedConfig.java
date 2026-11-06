package com.winter.cloud.xxljob.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/**
 * Extra beans for embedded xxl-job-admin
 */
@Configuration
public class XxlJobEmbedConfig {

    /** Fallback mail sender so alarm wiring does not fail when SMTP is unset */
    @Bean
    @ConditionalOnMissingBean(JavaMailSender.class)
    public JavaMailSender javaMailSender() {
        return new JavaMailSenderImpl();
    }
}