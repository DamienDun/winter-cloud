package com.winter.cloud.xxljob.service.impl;

import com.alibaba.fastjson2.JSON;
import com.winter.cloud.xxljob.domain.XxlJobGroup;
import com.winter.cloud.xxljob.domain.XxlJobRegistry;
import com.winter.cloud.xxljob.core.util.I18nUtil;
import com.winter.cloud.xxljob.mapper.XxlJobGroupMapper;
import com.winter.cloud.xxljob.mapper.XxlJobInfoMapper;
import com.winter.cloud.xxljob.mapper.XxlJobRegistryMapper;
import com.xxl.job.core.enums.RegistryConfig;
import com.winter.cloud.xxljob.service.IXxlJobGroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class XxlJobGroupServiceImpl implements IXxlJobGroupService {

    @Autowired
    private XxlJobGroupMapper xxlJobGroupDao;
    @Autowired
    private XxlJobInfoMapper xxlJobInfoDao;
    @Autowired
    private XxlJobRegistryMapper xxlJobRegistryDao;

    @Override
    public Map<String, Object> pageList(int pageNum, int pageSize, String appname, String title) {
        int start = Math.max(pageNum - 1, 0) * pageSize;
        List<XxlJobGroup> list = xxlJobGroupDao.pageList(start, pageSize, appname, title);
        int count = xxlJobGroupDao.pageListCount(start, pageSize, appname, title);
        Map<String, Object> result = new HashMap<>();
        result.put("total", count);
        result.put("rows", list);
        return result;
    }

    @Override
    public List<Map<String, Object>> listAll() {
        List<XxlJobGroup> all = xxlJobGroupDao.findAll();
        if (all == null) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (XxlJobGroup g : all) {
            rows.add(JSON.parseObject(JSON.toJSONString(g)));
        }
        return rows;
    }

    @Override
    public void save(Map<String, Object> group) {
        XxlJobGroup g = toGroup(group);
        validate(g, true);
        g.setUpdateTime(new Date());
        int ret = xxlJobGroupDao.save(g);
        if (ret <= 0) {
            throw new IllegalStateException("save jobgroup failed");
        }
    }

    @Override
    public void update(Map<String, Object> group) {
        XxlJobGroup g = toGroup(group);
        validate(g, false);
        if (g.getAddressType() == 0) {
            List<String> registryList = findRegistryByAppName(g.getAppname());
            String addressListStr = null;
            if (registryList != null && !registryList.isEmpty()) {
                Collections.sort(registryList);
                StringBuilder sb = new StringBuilder();
                for (String item : registryList) {
                    sb.append(item).append(",");
                }
                addressListStr = sb.substring(0, sb.length() - 1);
            }
            g.setAddressList(addressListStr);
        }
        g.setUpdateTime(new Date());
        int ret = xxlJobGroupDao.update(g);
        if (ret <= 0) {
            throw new IllegalStateException("update jobgroup failed");
        }
    }

    @Override
    public void remove(int id) {
        int count = xxlJobInfoDao.pageListCount(0, 10, id, -1, null, null, null);
        if (count > 0) {
            throw new IllegalStateException(I18nUtil.getString("jobgroup_del_limit_0"));
        }
        List<XxlJobGroup> allList = xxlJobGroupDao.findAll();
        if (allList != null && allList.size() == 1) {
            throw new IllegalStateException(I18nUtil.getString("jobgroup_del_limit_1"));
        }
        int ret = xxlJobGroupDao.remove(id);
        if (ret <= 0) {
            throw new IllegalStateException("remove jobgroup failed");
        }
    }

    private void validate(XxlJobGroup g, boolean creating) {
        if (g.getAppname() == null || g.getAppname().trim().isEmpty()) {
            throw new IllegalStateException(I18nUtil.getString("system_please_input") + "AppName");
        }
        if (g.getAppname().length() < 4 || g.getAppname().length() > 64) {
            throw new IllegalStateException(I18nUtil.getString("jobgroup_field_appname_length"));
        }
        if (g.getTitle() == null || g.getTitle().trim().isEmpty()) {
            throw new IllegalStateException(I18nUtil.getString("system_please_input") + I18nUtil.getString("jobgroup_field_title"));
        }
        if (g.getAddressType() != 0) {
            if (g.getAddressList() == null || g.getAddressList().trim().isEmpty()) {
                throw new IllegalStateException(I18nUtil.getString("jobgroup_field_addressType_limit"));
            }
        }
    }

    private List<String> findRegistryByAppName(String appnameParam) {
        Map<String, List<String>> appAddressMap = new HashMap<>();
        List<XxlJobRegistry> list = xxlJobRegistryDao.findAll(RegistryConfig.DEAD_TIMEOUT, new Date());
        if (list != null) {
            for (XxlJobRegistry item : list) {
                if (RegistryConfig.RegistType.EXECUTOR.name().equals(item.getRegistryGroup())) {
                    String appname = item.getRegistryKey();
                    List<String> registryList = appAddressMap.computeIfAbsent(appname, k -> new ArrayList<>());
                    if (!registryList.contains(item.getRegistryValue())) {
                        registryList.add(item.getRegistryValue());
                    }
                }
            }
        }
        return appAddressMap.get(appnameParam);
    }

    private XxlJobGroup toGroup(Map<String, Object> group) {
        return JSON.parseObject(JSON.toJSONString(group), XxlJobGroup.class);
    }
}