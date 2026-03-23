package com.winter.cloud.common.core.utils.tuple;

/**
 * @author Damien
 * @description 四个项目
 * @create 2022/8/8 9:52
 */
public class TupleFour<T1, T2, T3, T4> extends TupleThree<T1, T2, T3> {

    /**
     *
     */
    private static final long serialVersionUID = 5572700835850474957L;

    private final T4 item4;

    /**
     * 实例化 Pair
     *
     * @param item1 项目1
     * @param item2 项目2
     * @param item3 项目3
     * @param item4 项目4
     */
    public TupleFour(T1 item1, T2 item2, T3 item3, T4 item4) {
        super(item1, item2, item3);
        this.item4 = item4;
    }

    /**
     * 获取项目4
     *
     * @return
     */
    public T4 getItem4() {
        return item4;
    }
}
