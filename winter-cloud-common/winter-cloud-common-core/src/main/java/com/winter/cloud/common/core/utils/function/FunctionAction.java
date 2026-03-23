package com.winter.cloud.common.core.utils.function;

import java.io.Serializable;

/**
 * @author Damien
 * @description 无参数无返回值
 * @create 2022/8/8 13:07
 */
@FunctionalInterface
public interface FunctionAction extends Serializable {

    /**
     * 应用
     */
    void apply();
}
