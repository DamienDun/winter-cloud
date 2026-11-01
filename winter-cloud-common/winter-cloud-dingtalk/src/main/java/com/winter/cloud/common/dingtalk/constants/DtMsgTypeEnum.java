package com.winter.cloud.common.dingtalk.constants;

/**
 * 钉钉 工作通知类型 常量
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2022/8/19 19:41
 */
public enum DtMsgTypeEnum {

    TEXT("text"),
    IMAGE("image"),
    FILE("file"),
    LINK("link"),
    MARKDOWN("markdown"),
    OA("oa"),
    ACTION_CARD("action_card");

    private String name;

    DtMsgTypeEnum(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
