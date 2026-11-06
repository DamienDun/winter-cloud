package com.winter.cloud.xxljob.service;

import java.util.List;
import java.util.Map;

public interface IXxlJobLogService {
    Map<String, Object> pageList(int pageNum, int pageSize, Integer jobGroup, Integer jobId,
                                 Integer logStatus, String filterTime);
    List<Map<String, Object>> getJobsByGroup(int jobGroup);
    Map<String, Object> logDetailCat(String executorAddress, long triggerTime, long logId, int fromLineNum);
    void clearLog(int jobGroup, int jobId, int type);
    void kill(int id);
}
