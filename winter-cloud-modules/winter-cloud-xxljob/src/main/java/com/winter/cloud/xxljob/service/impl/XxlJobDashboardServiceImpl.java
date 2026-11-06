package com.winter.cloud.xxljob.service.impl;

import com.winter.cloud.xxljob.service.XxlJobService;
import com.xxl.job.core.biz.model.ReturnT;
import com.winter.cloud.xxljob.service.IXxlJobDashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class XxlJobDashboardServiceImpl implements IXxlJobDashboardService {

    @Autowired
    private XxlJobService xxlJobService;

    @Override
    public Map<String, Object> dashboardInfo() {
        return xxlJobService.dashboardInfo();
    }

    @Override
    public Map<String, Object> chartInfo(String startDate, String endDate) {
        Date start = parse(startDate);
        Date end = parse(endDate);
        ReturnT<Map<String, Object>> ret = xxlJobService.chartInfo(start, end);
        if (ret == null || ret.getCode() != ReturnT.SUCCESS_CODE) {
            throw new IllegalStateException(ret == null ? "chartInfo failed" : ret.getMsg());
        }
        Map<String, Object> content = ret.getContent();
        return content == null ? new LinkedHashMap<>() : content;
    }

    private Date parse(String s) {
        try {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(s);
        } catch (ParseException e) {
            throw new IllegalArgumentException("invalid date: " + s, e);
        }
    }
}