package com.winter.cloud.common.security.utils;

import org.springframework.stereotype.Component;
import com.winter.cloud.common.core.utils.poi.ExcelDictHandler;

/**
 * Excel 字典转换实现：委托 DictUtils 读取 Redis 字典缓存。
 *
 * @author winter
 */
@Component
public class DictExcelHandler implements ExcelDictHandler
{
    @Override
    public String getDictLabel(String dictType, String dictValue, String separator)
    {
        return DictUtils.getDictLabel(dictType, dictValue, separator);
    }

    @Override
    public String getDictValue(String dictType, String dictLabel, String separator)
    {
        return DictUtils.getDictValue(dictType, dictLabel, separator);
    }

    @Override
    public String[] getDictLabelArr(String dictType)
    {
        return DictUtils.getDictLabelArr(dictType);
    }
}