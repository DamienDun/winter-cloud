package com.winter.cloud.common.filestorage.support.validation;

import java.io.Serializable;

/** 数据校验接口 */
public interface DataValidation extends Serializable {
    void valid();
}