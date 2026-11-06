package com.winter.cloud.xxljob.core.conf;

import com.winter.cloud.xxljob.core.alarm.JobAlarmer;
import com.winter.cloud.xxljob.core.scheduler.XxlJobScheduler;
import com.winter.cloud.xxljob.mapper.*;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.util.Arrays;

/**
 * xxl-job config
 *
 * @author xuxueli 2017-04-28
 */

@Component
public class XxlJobAdminConfig implements InitializingBean, DisposableBean {

    private static XxlJobAdminConfig adminConfig = null;
    public static XxlJobAdminConfig getAdminConfig() {
        return adminConfig;
    }


    // ---------------------- XxlJobScheduler ----------------------

    private XxlJobScheduler xxlJobScheduler;

    @Override
    public void afterPropertiesSet() throws Exception {
        adminConfig = this;

        xxlJobScheduler = new XxlJobScheduler();
        xxlJobScheduler.init();
    }

    @Override
    public void destroy() throws Exception {
        xxlJobScheduler.destroy();
    }


    // ---------------------- XxlJobScheduler ----------------------

    // conf
    @Value("${xxl.job.i18n:zh_CN}")
    private String i18n;

    @Value("${xxl.job.accessToken:}")
    private String accessToken;

    @Value("${spring.mail.from:xxljob@localhost}")
    private String emailFrom;

    @Value("${xxl.job.triggerpool.fast.max:200}")
    private int triggerPoolFastMax;

    @Value("${xxl.job.triggerpool.slow.max:100}")
    private int triggerPoolSlowMax;

    @Value("${xxl.job.logretentiondays:30}")
    private int logretentiondays;

    // dao, service

    @Resource
    private XxlJobLogMapper xxlJobLogDao;
    @Resource
    private XxlJobInfoMapper xxlJobInfoDao;
    @Resource
    private XxlJobRegistryMapper xxlJobRegistryDao;
    @Resource
    private XxlJobGroupMapper xxlJobGroupDao;
    @Resource
    private XxlJobLogReportMapper xxlJobLogReportDao;
    @Resource
    private JavaMailSender mailSender;
    @Resource
    private DataSource dataSource;
    @Resource
    private JobAlarmer jobAlarmer;


    public String getI18n() {
        if (!Arrays.asList("zh_CN", "zh_TC", "en").contains(i18n)) {
            return "zh_CN";
        }
        return i18n;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getEmailFrom() {
        return emailFrom;
    }

    public int getTriggerPoolFastMax() {
        if (triggerPoolFastMax < 200) {
            return 200;
        }
        return triggerPoolFastMax;
    }

    public int getTriggerPoolSlowMax() {
        if (triggerPoolSlowMax < 100) {
            return 100;
        }
        return triggerPoolSlowMax;
    }

    public int getLogretentiondays() {
        if (logretentiondays < 7) {
            return -1;  // Limit greater than or equal to 7, otherwise close
        }
        return logretentiondays;
    }

    public XxlJobLogMapper getXxlJobLogMapper() {
        return xxlJobLogDao;
    }

    public XxlJobInfoMapper getXxlJobInfoMapper() {
        return xxlJobInfoDao;
    }

    public XxlJobRegistryMapper getXxlJobRegistryMapper() {
        return xxlJobRegistryDao;
    }

    public XxlJobGroupMapper getXxlJobGroupMapper() {
        return xxlJobGroupDao;
    }

    public XxlJobLogReportMapper getXxlJobLogReportMapper() {
        return xxlJobLogReportDao;
    }

    public JavaMailSender getMailSender() {
        return mailSender;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public JobAlarmer getJobAlarmer() {
        return jobAlarmer;
    }

}
