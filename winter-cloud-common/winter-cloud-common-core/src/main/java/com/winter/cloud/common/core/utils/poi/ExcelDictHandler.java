package com.winter.cloud.common.core.utils.poi;

/**
 * Excel 字典转换 SPI。
 * <p>
 * common-core 不依赖 Redis / system，由 common-security 提供实现并注册为 Spring Bean。
 * 未注册时 {@link ExcelUtil} 对 dictType 不做转换。
 * </p>
 *
 * @author winter
 */
public interface ExcelDictHandler
{
    /**
     * 字典值转标签（导出）
     *
     * @param dictType  字典类型
     * @param dictValue 字典值，多值用 separator 分隔
     * @param separator 分隔符
     * @return 字典标签
     */
    String getDictLabel(String dictType, String dictValue, String separator);

    /**
     * 字典标签转值（导入）
     *
     * @param dictType  字典类型
     * @param dictLabel 字典标签，多值用 separator 分隔
     * @param separator 分隔符
     * @return 字典值
     */
    String getDictValue(String dictType, String dictLabel, String separator);

    /**
     * 按字典类型获取全部标签（导入模板下拉）
     *
     * @param dictType 字典类型
     * @return 标签数组，无数据时返回空数组
     */
    String[] getDictLabelArr(String dictType);
}
