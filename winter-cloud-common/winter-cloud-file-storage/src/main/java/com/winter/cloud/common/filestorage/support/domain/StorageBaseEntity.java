package com.winter.cloud.common.filestorage.support.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
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

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private String remark;

    private Date createTime;

    private Date updateTime;
}