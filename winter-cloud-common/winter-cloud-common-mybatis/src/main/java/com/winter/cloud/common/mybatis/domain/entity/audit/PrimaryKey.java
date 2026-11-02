package com.winter.cloud.common.mybatis.domain.entity.audit;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import java.io.Serializable;

/** 主键 */
public interface PrimaryKey<TKey extends Serializable> extends Serializable {

    String FIELD_ID = "id";
    String COLUMN_ID = "id";

    @JsonSerialize(using = ToStringSerializer.class)
    TKey getId();

    void setId(TKey id);
}
