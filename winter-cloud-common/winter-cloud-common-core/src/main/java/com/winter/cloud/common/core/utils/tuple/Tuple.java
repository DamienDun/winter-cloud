package com.winter.cloud.common.core.utils.tuple;

import java.io.Serializable;

/**
 * @author Damien
 * @description 单个
 * @create 2022/8/8 9:52
 */
public class Tuple<T1> implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = -4340953436576826848L;
    private final T1 item1;

    /**
     * 实例化 Unit 类
     *
     * @param item1 项目1
     */
    public Tuple(T1 item1) {
        this.item1 = item1;
    }

    /**
     * 获取项目1
     *
     * @return
     */
    public T1 getItem1() {
        return item1;
    }

}
