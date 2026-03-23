package com.winter.cloud.common.core.utils.tuple;

/**
 * @author Damien
 * @description 两个
 * @create 2022/8/8 9:52
 */
public class TupleTwo<T1, T2> extends Tuple<T1> {

    /**
     *
     */
    private static final long serialVersionUID = 8370774750834888215L;
    private final T2 item2;

    /**
     * 实例化 Pair
     *
     * @param item1 项目1
     * @param item2 项目2
     */
    public TupleTwo(T1 item1, T2 item2) {
        super(item1);
        this.item2 = item2;
    }

    /**
     * 获取项目2
     *
     * @return
     */
    public T2 getItem2() {
        return item2;
    }

}
