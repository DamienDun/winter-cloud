package com.winter.cloud.common.mybatis.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.winter.cloud.common.core.context.SecurityContextHolder;
import com.winter.cloud.common.core.utils.DateUtils;
import com.winter.cloud.common.mybatis.domain.entity.audit.CreateAuditing;
import com.winter.cloud.common.mybatis.domain.entity.audit.DeleteAuditing;
import com.winter.cloud.common.mybatis.domain.entity.audit.ModifiedAuditing;
import com.winter.cloud.common.core.enums.CommonEnum;
import org.apache.ibatis.reflection.MetaObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Objects;

/**
 * 审计字段自动填充（创建/修改/逻辑删除）
 */
public class MetaObjectHandlerConfig implements MetaObjectHandler {

    private static final Logger log = LoggerFactory.getLogger(MetaObjectHandlerConfig.class);

    @Override
    public void insertFill(MetaObject metaObject) {
        Date nowDate = DateUtils.getNowDate();
        Long userId = currentUserId();
        if (CreateAuditing.class.isAssignableFrom(metaObject.getOriginalObject().getClass())) {
            if (Objects.isNull(getFieldValByName(CreateAuditing.FIELD_GMT_CREATE, metaObject))) {
                setFieldValByName(CreateAuditing.FIELD_GMT_CREATE, nowDate, metaObject);
            }
            if (userId != null && Objects.isNull(getFieldValByName(CreateAuditing.FIELD_CREATED_USER_ID, metaObject))) {
                setFieldValByName(CreateAuditing.FIELD_CREATED_USER_ID, userId, metaObject);
            }
        }
        if (ModifiedAuditing.class.isAssignableFrom(metaObject.getOriginalObject().getClass())) {
            if (Objects.isNull(getFieldValByName(ModifiedAuditing.FIELD_GMT_MODIFIED, metaObject))) {
                setFieldValByName(ModifiedAuditing.FIELD_GMT_MODIFIED, nowDate, metaObject);
            }
            if (userId != null && Objects.isNull(getFieldValByName(ModifiedAuditing.FIELD_MODIFIED_USER_ID, metaObject))) {
                setFieldValByName(ModifiedAuditing.FIELD_MODIFIED_USER_ID, userId, metaObject);
            }
        }
    }

    /**
     * 逻辑删除时需传入实体（仅传 id 不会触发删除审计填充）
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        Long userId = currentUserId();
        Date nowDate = DateUtils.getNowDate();
        if (metaObject.hasGetter(DeleteAuditing.FIELD_DELETED)
                && CommonEnum.Deleted.TRUE.getCode().equals(metaObject.getValue(DeleteAuditing.FIELD_DELETED))) {
            if (DeleteAuditing.class.isAssignableFrom(metaObject.getOriginalObject().getClass())) {
                setFieldValByName(DeleteAuditing.FIELD_GMT_DELETE, nowDate, metaObject);
                if (userId != null) {
                    setFieldValByName(DeleteAuditing.FIELD_DELETED_USER_ID, userId, metaObject);
                }
            }
        } else if (CreateAuditing.class.isAssignableFrom(metaObject.getOriginalObject().getClass())) {
            setFieldValByName(ModifiedAuditing.FIELD_GMT_MODIFIED, nowDate, metaObject);
            if (userId != null) {
                setFieldValByName(ModifiedAuditing.FIELD_MODIFIED_USER_ID, userId, metaObject);
            }
        }
    }

    private Long currentUserId() {
        try {
            return SecurityContextHolder.getUserId();
        } catch (Exception e) {
            log.debug("获取不到当前登录用户，跳过审计用户填充");
            return null;
        }
    }
}
