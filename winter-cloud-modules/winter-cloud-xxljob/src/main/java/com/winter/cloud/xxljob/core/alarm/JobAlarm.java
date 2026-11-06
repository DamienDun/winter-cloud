package com.winter.cloud.xxljob.core.alarm;

import com.winter.cloud.xxljob.domain.XxlJobInfo;
import com.winter.cloud.xxljob.domain.XxlJobLog;

/**
 * @author xuxueli 2020-01-19
 */
public interface JobAlarm {

    /**
     * job alarm
     *
     * @param info
     * @param jobLog
     * @return
     */
    public boolean doAlarm(XxlJobInfo info, XxlJobLog jobLog);

}
