package com.winter.cloud.xxljob.dto;

import java.util.List;

/**
 * @author LinYingDun
 * @description 统计待执行任务的入参
 * @create 2022/4/8 11:55
 */
public class StatsPendingJobInput {

    /**
     * cron表达式列表
     */
    private List<String> cronList;

    public List<String> getCronList() {
        return cronList;
    }

    public void setCronList(List<String> cronList) {
        this.cronList = cronList;
    }

    public StatsPendingJobInput() {
    }
}
