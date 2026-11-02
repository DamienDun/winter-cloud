package com.winter.cloud.common.mybatis.domain.entity.audit;

import java.util.Date;

/** 修改审计 */
public interface ModifiedAuditing {

    String FIELD_GMT_MODIFIED = "gmtModified";
    String COLUMN_GMT_MODIFIED = "gmt_modified";
    String FIELD_MODIFIED_USER_ID = "modifiedUserId";
    String COLUMN_MODIFIED_USER_ID = "modified_user_id";

    Date getGmtModified();
    void setGmtModified(Date gmtModified);
    Long getModifiedUserId();
    void setModifiedUserId(Long modifiedUserId);
}
