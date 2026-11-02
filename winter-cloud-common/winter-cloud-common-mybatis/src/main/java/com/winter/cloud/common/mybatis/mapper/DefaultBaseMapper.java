package com.winter.cloud.common.mybatis.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 默认基础 Mapper：批量插入/替换/更新、逻辑删除填充、取首条
 */
public interface DefaultBaseMapper<T> extends BaseMapper<T> {

    String METHOD_BATCH_DELETE_WITH_FILL = "batchDeleteWithFill";

    int batchDeleteWithFill(@Param(Constants.COLLECTION) Collection<?> idList);

    int insertBatch(@Param(Constants.COLLECTION) Collection<?> entities);

    int replaceBatch(@Param(Constants.COLLECTION) Collection<?> entities);

    int updateBatchById(@Param(Constants.COLLECTION) Collection<?> entities,
                        @Param(Constants.WRAPPER) Wrapper<T> updateWrapper);

    default int insertInBatch(Collection<?> entities, int batchSize) {
        int frequency = entities.size() / batchSize;
        int count = 0;
        for (int i = 0; i < frequency; i++) {
            count += insertBatch(entities.stream().skip((long) i * batchSize).limit(batchSize).collect(Collectors.toList()));
        }
        int lastCount = entities.size() - frequency * batchSize;
        if (lastCount > 0) {
            count += insertBatch(entities.stream().skip((long) frequency * batchSize).limit(lastCount).collect(Collectors.toList()));
        }
        return count;
    }

    default int replaceInBatch(Collection<?> entities, int batchSize) {
        int frequency = entities.size() / batchSize;
        int count = 0;
        for (int i = 0; i < frequency; i++) {
            count += replaceBatch(entities.stream().skip((long) i * batchSize).limit(batchSize).collect(Collectors.toList()));
        }
        int lastCount = entities.size() - frequency * batchSize;
        if (lastCount > 0) {
            count += replaceBatch(entities.stream().skip((long) frequency * batchSize).limit(lastCount).collect(Collectors.toList()));
        }
        return count;
    }

    default int updateInBatch(Collection<?> entities, Wrapper<T> updateWrapper, int batchSize) {
        int frequency = entities.size() / batchSize;
        int count = 0;
        for (int i = 0; i < frequency; i++) {
            count += updateBatchById(entities.stream().skip((long) i * batchSize).limit(batchSize).collect(Collectors.toList()), updateWrapper);
        }
        int lastCount = entities.size() - frequency * batchSize;
        if (lastCount > 0) {
            count += updateBatchById(entities.stream().skip((long) frequency * batchSize).limit(lastCount).collect(Collectors.toList()), updateWrapper);
        }
        return count;
    }

    default T selectForFirst(@Param(Constants.WRAPPER) Wrapper<T> queryWrapper) {
        List<T> list = selectList(queryWrapper);
        if (CollectionUtils.isNotEmpty(list)) {
            return list.get(0);
        }
        return null;
    }
}
