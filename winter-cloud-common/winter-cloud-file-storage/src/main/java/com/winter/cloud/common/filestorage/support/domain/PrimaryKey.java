package com.winter.cloud.common.filestorage.support.domain;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import java.io.Serializable;

/** 主键接口 */
public interface PrimaryKey<T extends Serializable> extends Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    T getId();

    void setId(T id);
}