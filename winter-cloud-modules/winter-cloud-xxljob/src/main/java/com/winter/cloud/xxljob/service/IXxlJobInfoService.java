package com.winter.cloud.xxljob.service;

import java.util.List;
import java.util.Map;

public interface IXxlJobInfoService {
    Map<String, Object> pageList(int pageNum, int pageSize, Integer jobGroup, Integer triggerStatus,
                                 String jobDesc, String executorHandler, String author);
    Map<String, Object> loadDetail(int id);
    void add(Map<String, Object> job);
    void update(Map<String, Object> job);
    void remove(int id);
    void start(int id);
    void stop(int id);
    void trigger(int id, String executorParam, String addressList);
    List<String> nextTriggerTime(String scheduleType, String scheduleConf);
    /** GLUE IDE：保存源码并写入历史 */
    void saveGlue(int id, String glueSource, String glueRemark);
}
