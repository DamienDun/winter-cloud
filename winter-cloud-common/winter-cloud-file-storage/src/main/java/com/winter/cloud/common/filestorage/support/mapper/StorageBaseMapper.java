package com.winter.cloud.common.filestorage.support.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 存储模块 Mapper 基座 */
public interface StorageBaseMapper<T> extends BaseMapper<T> {

    default T selectForFirst(@Param(Constants.WRAPPER) Wrapper<T> queryWrapper) {
        List<T> list = selectList(queryWrapper);
        if (CollectionUtils.isNotEmpty(list)) {
            return list.get(0);
        }
        return null;
    }
}