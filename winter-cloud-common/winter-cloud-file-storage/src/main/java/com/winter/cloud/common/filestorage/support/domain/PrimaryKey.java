package com.winter.cloud.common.filestorage.support.domain;

import java.io.Serializable;

/** 主键接口 */
public interface PrimaryKey<T extends Serializable> extends Serializable {
    T getId();
    void setId(T id);
}