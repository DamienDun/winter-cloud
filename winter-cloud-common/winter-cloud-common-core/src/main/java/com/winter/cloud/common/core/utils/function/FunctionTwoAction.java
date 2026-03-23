package com.winter.cloud.common.core.utils.function;

import java.io.Serializable;

/**
 * @author Damien
 * @description 具有2个参数和无返回值函数
 * @create 2022/7/20 8:55
 */
@FunctionalInterface
public interface FunctionTwoAction<T1, T2> extends Serializable {

    /**
     * 应用
     *
     * @param t1 参数1
     * @param t2 参数2
     */
    void apply(T1 t1, T2 t2);
}
