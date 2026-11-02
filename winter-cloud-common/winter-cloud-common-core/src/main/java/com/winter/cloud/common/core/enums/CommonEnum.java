package com.winter.cloud.common.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** 通用枚举 */
public interface CommonEnum extends BaseEnum {

    @Getter
    @AllArgsConstructor
    enum Deleted implements CommonEnum {
        FALSE(0, "未删"),
        TRUE(1, "删除");

        private final Integer code;
        private final String msg;
    }
}
