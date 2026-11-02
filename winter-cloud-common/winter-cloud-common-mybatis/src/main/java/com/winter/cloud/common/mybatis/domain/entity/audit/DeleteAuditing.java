package com.winter.cloud.common.mybatis.domain.entity.audit;

import java.util.Date;

/** 删除审计 */
public interface DeleteAuditing {

    String COLUMN_DELETED = "is_deleted";
    String FIELD_DELETED = "deleted";
    String FIELD_GMT_DELETE = "gmtDelete";
    String COLUMN_GMT_DELETE = "gmt_delete";
    String FIELD_DELETED_USER_ID = "deletedUserId";
    String COLUMN_DELETED_USER_ID = "deleted_user_id";

    Long getDeletedUserId();
    void setDeletedUserId(Long deletedUserId);
    Date getGmtDelete();
    void setGmtDelete(Date gmtDelete);
    Integer getDeleted();
    void setDeleted(Integer deleted);
}
