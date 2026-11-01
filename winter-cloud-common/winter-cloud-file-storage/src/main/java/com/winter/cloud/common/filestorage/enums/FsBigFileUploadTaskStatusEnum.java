package com.winter.cloud.common.filestorage.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2023/12/14 9:28
 */
@Getter
@AllArgsConstructor
public enum FsBigFileUploadTaskStatusEnum {

    CANCEL(0, "取消"),
    NEW(1, "新建"),
    UPLOADING(2, "上传中"),
    COMPLETE_UPLOAD(3, "上传完成"),
    ;
    private Integer code;
    private String msg;

}
