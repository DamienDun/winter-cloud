package com.winter.cloud.common.filestorage;

import java.io.Serializable;

/**
 * 分区信息抽象
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2022/8/15 20:48
 */
public abstract class AbstractBucket implements Serializable {

    private static final long serialVersionUID = 2761765610399715980L;

    /**
     * 分区名称
     */
    private final String name;

    /**
     * @param name
     */
    public AbstractBucket(String name) {
        this.name = name;
    }

    /**
     * 获取分区名称
     *
     * @return
     */
    public final String getName() {
        return this.name;
    }

    @Override
    public String toString() {
        return "name = " + this.getName();
    }

}
