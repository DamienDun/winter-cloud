package com.winter.cloud.xxljob.service;

import java.util.List;
import java.util.Map;

public interface IXxlJobGroupService {
    Map<String, Object> pageList(int pageNum, int pageSize, String appname, String title);
    List<Map<String, Object>> listAll();
    void save(Map<String, Object> group);
    void update(Map<String, Object> group);
    void remove(int id);
}
