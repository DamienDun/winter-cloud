package com.winter.cloud.common.mybatis.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.winter.cloud.common.mybatis.domain.entity.audit.CreateAuditing;
import com.winter.cloud.common.mybatis.domain.entity.audit.DeleteAuditing;
import com.winter.cloud.common.mybatis.domain.entity.audit.Entity;
import com.winter.cloud.common.mybatis.domain.entity.audit.ModifiedAuditing;
import com.winter.cloud.common.core.enums.CommonEnum;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

/**
 * MyBatis-Plus 默认基础实体（gmt_* / 逻辑删除 / 审计用户）
 */
@Getter
@Setter
@NoArgsConstructor
public class DefaultBaseEntity implements Entity<Long>, CreateAuditing, ModifiedAuditing, DeleteAuditing {

    private static final long serialVersionUID = 5229461843731683997L;

    /** 雪花ID必须序列化成字符串，否则前端 Number 丢精度导致回显/请求用错ID */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String remark;

    @TableLogic(value = "0", delval = "1")
    @TableField(value = DeleteAuditing.COLUMN_DELETED)
    private Integer deleted = CommonEnum.Deleted.FALSE.getCode();

    @TableField(fill = FieldFill.INSERT, updateStrategy = FieldStrategy.NEVER)
    private Date gmtCreate;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date gmtModified;

    @TableField(fill = FieldFill.UPDATE)
    private Date gmtDelete;

    @TableField(fill = FieldFill.INSERT, updateStrategy = FieldStrategy.NEVER)
    private Long createdUserId;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long modifiedUserId;

    @TableField(fill = FieldFill.UPDATE)
    private Long deletedUserId;
}
