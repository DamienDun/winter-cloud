package com.winter.cloud.file.enums;

import com.winter.cloud.common.core.utils.StringUtils;

/**
 * 资源文件类型（对齐 portal SmsEnum.FileType）
 */
public enum ResourceFileTypeEnum {
    OTHER(0),
    PIC(1),
    AUDIO(2),
    VIDEO(3),
    PDF(4),
    DOC(5),
    EXCEL(6),
    PPT(7),
    TXT(8),
    HTML(9);

    private final int code;

    ResourceFileTypeEnum(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static int resolveByExtension(String extension) {
        if (StringUtils.isEmpty(extension)) {
            return OTHER.code;
        }
        String ext = extension.toLowerCase();
        if (ext.matches("jpg|jpeg|png|gif|bmp|webp|svg")) {
            return PIC.code;
        }
        if (ext.matches("mp3|wav|flac|aac|ogg|m4a")) {
            return AUDIO.code;
        }
        if (ext.matches("mp4|avi|mkv|mov|wmv|flv|webm")) {
            return VIDEO.code;
        }
        if ("pdf".equals(ext)) {
            return PDF.code;
        }
        if (ext.matches("doc|docx")) {
            return DOC.code;
        }
        if (ext.matches("xls|xlsx|csv")) {
            return EXCEL.code;
        }
        if (ext.matches("ppt|pptx")) {
            return PPT.code;
        }
        if ("txt".equals(ext)) {
            return TXT.code;
        }
        if (ext.matches("html|htm")) {
            return HTML.code;
        }
        return OTHER.code;
    }
}