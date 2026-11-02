package com.winter.cloud.common.mybatis.domain.entity.audit;

import java.util.Date;

/** 创建审计 */
public interface CreateAuditing extends Auditing {

    String FIELD_GMT_CREATE = "gmtCreate";
    String COLUMN_GMT_CREATE = "gmt_create";
    String FIELD_CREATED_USER_ID = "createdUserId";
    String COLUMN_CREATED_USER_ID = "created_user_id";

    Date getGmtCreate();
    void setGmtCreate(Date gmtCreate);
    Long getCreatedUserId();
    void setCreatedUserId(Long createdUserId);
}
