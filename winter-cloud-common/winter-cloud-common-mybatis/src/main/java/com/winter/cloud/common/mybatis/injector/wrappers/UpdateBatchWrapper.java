package com.winter.cloud.common.mybatis.injector.wrappers;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.core.conditions.AbstractLambdaWrapper;
import com.baomidou.mybatisplus.core.toolkit.Assert;
import com.baomidou.mybatisplus.core.toolkit.LambdaUtils;
import com.baomidou.mybatisplus.core.toolkit.support.ColumnCache;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 批量更新wrapper
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2022/9/13 15:59
 */
public class UpdateBatchWrapper<T> extends AbstractLambdaWrapper<T, UpdateBatchWrapper<T>> {

    private static final long serialVersionUID = -5574427909716866088L;

    private Map<String, ColumnCache> columnMap = null;
    private boolean initColumnMap = false;

    /**
     * 需要更新的字段
     */
    private List<String> updateFields = null;

    public UpdateBatchWrapper() {
        this.updateFields = new ArrayList<>();
    }

    public UpdateBatchWrapper(Class<?> entityClass) {
        this.updateFields = getColumnCaches(entityClass).stream().map(ColumnCache::getColumn).collect(Collectors.toList());
    }

    @Override
    protected UpdateBatchWrapper<T> instance() {
        this.updateFields = new ArrayList<>();
        return this;
    }

    /**
     * 关键代码,为属性设置值
     */
    @SafeVarargs
    public final UpdateBatchWrapper<T> setUpdateFields(SFunction<T, ?>... columns) {
        List<String> newUpdateFields = Arrays.asList(columnsToString(columns).split(","));
        this.updateFields.clear();
        newUpdateFields.forEach(field -> this.updateFields.add(field));
        return this;
    }

    public List<String> getUpdateFields() {
        return updateFields;
    }

    private void tryInitCache(Class<?> lambdaClass) {
        if (!initColumnMap) {
            final Class<T> entityClass = getEntityClass();
            if (entityClass != null) {
                lambdaClass = entityClass;
            }
            columnMap = LambdaUtils.getColumnMap(lambdaClass);
            Assert.notNull(columnMap, "can not find lambda cache for this entity [%s]", lambdaClass.getName());
            initColumnMap = true;
        }
    }

    protected List<ColumnCache> getColumnCaches(Class<?> entityClass) {
        Field[] fields = entityClass.getDeclaredFields();
        tryInitCache(entityClass);
        List<ColumnCache> columnCaches = new ArrayList<>(fields.length);
        for (Field field : fields) {
            TableField tableFieldAnno = field.getAnnotation(TableField.class);
            // 使用了注解 非表字段
            if (tableFieldAnno != null && !tableFieldAnno.exist()) {
                continue;
            }
            columnCaches.add(getColumnCache(field.getName(), entityClass));
        }
        return columnCaches;
    }

    private ColumnCache getColumnCache(String fieldName, Class<?> lambdaClass) {
        ColumnCache columnCache = columnMap.get(LambdaUtils.formatKey(fieldName));
        Assert.notNull(columnCache, "can not find lambda cache for this property [%s] of entity [%s]",
                fieldName, lambdaClass.getName());
        return columnCache;
    }

    /**
     * 添加要更新的字段(如果不存在)
     *
     * @param column
     */
    public void addFieldIfAbsent(SFunction<T, ?> column) {
        String columnName = columnToString(column);
        if (!getUpdateFields().contains(columnName)) {
            getUpdateFields().add(columnName);
        }
    }

    /**
     * 添加要更新的字段(如果不存在)
     *
     * @param columns
     */
    public void addFieldIfAbsent(SFunction<T, ?>... columns) {
        List<String> columnNames = Arrays.asList(columnsToString(columns).split(","));
        columnNames.forEach(columnName -> {
            if (!getUpdateFields().contains(columnName)) {
                getUpdateFields().add(columnName);
            }
        });
    }

    /**
     * 添加要更新的字段(如果不存在)
     *
     * @param columnNames 列名
     */
    public void addFieldIfAbsent(String... columnNames) {
        for (String columnName : columnNames) {
            if (!getUpdateFields().contains(columnName)) {
                getUpdateFields().add(columnName);
            }
        }
    }

    /**
     * 配置不想更新的字段(如果不存在)
     *
     * @param columns
     */
    public void delFields(SFunction<T, ?>... columns) {
        List<String> columnNames = Arrays.asList(columnsToString(columns).split(","));
        columnNames.forEach(columnName -> getUpdateFields().remove(columnName));
    }
}

