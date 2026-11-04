package com.winter.cloud.common.datasource.enums;

import com.winter.cloud.common.datasource.enums.BaseResultEnum;

/**
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2023/4/20 15:20
 */
public enum DsResultEnum implements BaseResultEnum {

    DS_TYPE_NOT_EXIST(50001, "不存在该数据源类型"),
    DS_CONN_DB_ERROR(50002, "数据库连接失败,请检查配置信息"),
    ;


    private Integer code;

    private String msg;

    DsResultEnum(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    @Override
    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    @Override
    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    @Override
    public String getModule() {
        return "数据源模块";
    }
}
