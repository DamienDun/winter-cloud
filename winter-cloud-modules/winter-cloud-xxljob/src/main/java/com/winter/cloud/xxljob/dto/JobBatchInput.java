package com.winter.cloud.xxljob.dto;

import java.util.List;

/**
 * @author LinYingDun
 * @description 任务批量处理入参
 * @create 2022/6/14 9:53
 */
public class JobBatchInput {

    /**
     * 任务id列表
     */
    private List<Long> ids;

    public List<Long> getIds() {
        return ids;
    }

    public void setIds(List<Long> ids) {
        this.ids = ids;
    }

    public JobBatchInput() {
    }
}
