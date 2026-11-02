package com.winter.cloud.common.mybatis.configure;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.core.injector.ISqlInjector;
import com.winter.cloud.common.mybatis.config.MetaObjectHandlerConfig;
import com.winter.cloud.common.mybatis.config.MyBatisPlusSqlInjector;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 基础能力自动装配
 */
@Configuration
@ConditionalOnClass(name = "com.baomidou.mybatisplus.core.mapper.BaseMapper")
public class WinterMybatisAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(MetaObjectHandler.class)
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandlerConfig();
    }

    @Bean
    @ConditionalOnMissingBean(ISqlInjector.class)
    public ISqlInjector mybatisPlusSqlInjector() {
        return new MyBatisPlusSqlInjector();
    }
}
