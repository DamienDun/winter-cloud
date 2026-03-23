package com.winter.cloud.common.core.utils.tuple;

/**
 * @author Damien
 * @description 3个
 * @create 2022/8/8 9:52
 */
public class TupleThree<T1, T2, T3> extends TupleTwo<T1, T2> {

    /**
     *
     */
    private static final long serialVersionUID = -3898669224281171101L;
    private final T3 item3;

    /**
     * 实例化 Pair
     *
     * @param item1 项目1
     * @param item2 项目2
     * @param item3 项目3
     */
    public TupleThree(T1 item1, T2 item2, T3 item3) {
        super(item1, item2);
        this.item3 = item3;
    }

    /**
     * 获取项目3
     *
     * @return
     */
    public T3 getItem3() {
        return item3;
    }
}
