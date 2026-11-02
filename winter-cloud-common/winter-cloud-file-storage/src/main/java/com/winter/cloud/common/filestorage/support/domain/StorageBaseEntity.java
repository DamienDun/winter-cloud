package com.winter.cloud.common.filestorage.support.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

/** 存储模块基础实体 */
@Getter
@Setter
@NoArgsConstructor
public class StorageBaseEntity implements PrimaryKey<Long>, Serializable {

    private static final long serialVersionUID = 1L;

    /** 雪花ID，序列化为字符串，避免前端 JS 精度丢失 */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String remark;

    private Date createTime;

    private Date updateTime;
}