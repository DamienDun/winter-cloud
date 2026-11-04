package com.winter.cloud.common.datasource.exception;

import com.winter.cloud.common.core.constant.Constants;
import com.winter.cloud.common.core.enums.BaseResultEnum;

/**
 * 数据源类型异常
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2023/4/20 15:16
 */
public class DatasourceTypeException extends RuntimeException {

    private static final long serialVersionUID = -3305273892351939325L;

    private BaseResultEnum resultEnum;

    public DatasourceTypeException() {
    }

    public DatasourceTypeException(BaseResultEnum resultEnum) {
        super(resultEnum.getMsg());
        this.resultEnum = resultEnum;
    }

    public DatasourceTypeException(BaseResultEnum resultEnum, String... appendMsgs) {
        super(joinMsg(resultEnum.getMsg(), appendMsgs));
        this.resultEnum = resultEnum;
    }

    private static String joinMsg(String msg, String... appendMsgs) {
        StringBuilder sb = new StringBuilder();
        sb.append(msg);
        for (String appendMsg : appendMsgs) {
            sb.append(Constants.COLON).append(appendMsg);
        }
        return sb.toString();
    }

    public BaseResultEnum getResultEnum() {
        return resultEnum;
    }

    public void setResultEnum(BaseResultEnum resultEnum) {
        this.resultEnum = resultEnum;
    }
}

