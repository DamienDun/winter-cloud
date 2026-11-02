package com.winter.cloud.common.mybatis.config;

import com.baomidou.mybatisplus.core.injector.AbstractMethod;
import com.baomidou.mybatisplus.core.injector.DefaultSqlInjector;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.extension.injector.methods.LogicDeleteBatchByIds;
import com.winter.cloud.common.mybatis.injector.methods.InsertBatchMethod;
import com.winter.cloud.common.mybatis.injector.methods.ReplaceBatchMethod;
import com.winter.cloud.common.mybatis.injector.methods.UpdateBatchMethod;
import com.winter.cloud.common.mybatis.mapper.DefaultBaseMapper;

import java.util.List;

/**
 * 注入批量 insert/replace/update 与带填充的批量逻辑删除
 */
public class MyBatisPlusSqlInjector extends DefaultSqlInjector {

    @Override
    public List<AbstractMethod> getMethodList(Class<?> mapperClass, TableInfo tableInfo) {
        List<AbstractMethod> methodList = super.getMethodList(mapperClass, tableInfo);
        methodList.add(new LogicDeleteBatchByIds(DefaultBaseMapper.METHOD_BATCH_DELETE_WITH_FILL));
        methodList.add(new InsertBatchMethod());
        methodList.add(new ReplaceBatchMethod());
        methodList.add(new UpdateBatchMethod());
        return methodList;
    }
}
