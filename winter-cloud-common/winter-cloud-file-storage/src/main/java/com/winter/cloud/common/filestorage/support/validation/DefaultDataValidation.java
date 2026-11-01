package com.winter.cloud.common.filestorage.support.validation;

/** 默认数据校验（委托 javax.validation） */
public class DefaultDataValidation implements DataValidation {

    private static final long serialVersionUID = 1L;

    @Override
    public void valid() {
        ValidationUtils.validation(this);
    }
}