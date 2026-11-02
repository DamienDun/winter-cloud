package com.winter.cloud.common.mybatis.domain.entity.audit;

import java.io.Serializable;

/** 带逻辑删除的实体 */
public interface Entity<TKey extends Serializable> extends PrimaryKey<TKey>, DeleteAuditing {
}
