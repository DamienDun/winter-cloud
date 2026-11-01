package com.winter.cloud.common.filestorage.support.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** 字节工具 */
public final class ByteUtils {

    public static final long KB_SIZE = 1024;
    public static final long MB_SIZE = KB_SIZE * 1024;
    public static final long GB_SIZE = MB_SIZE * 1024;
    public static final long TB_SIZE = GB_SIZE * 1024;
    public static final long EB_SIZE = TB_SIZE * 1024;

    private ByteUtils() {
    }

    public static String getFileSize(Long value) {
        if (value == null || value <= 0) {
            return "0 KB";
        }
        long v = value;
        if (v < KB_SIZE) {
            return String.format("%s B", value);
        } else if (v < MB_SIZE) {
            return calcValue(v, KB_SIZE, "KB");
        } else if (v < GB_SIZE) {
            return calcValue(v, MB_SIZE, "MB");
        } else if (v < TB_SIZE) {
            return calcValue(v, GB_SIZE, "GB");
        } else if (v < EB_SIZE) {
            return calcValue(v, TB_SIZE, "TB");
        }
        return calcValue(v, EB_SIZE, "EB");
    }

    private static String calcValue(long value, long unit, String unitName) {
        BigDecimal result = BigDecimal.valueOf(value).divide(BigDecimal.valueOf(unit), 2, RoundingMode.HALF_UP);
        return result.stripTrailingZeros().toPlainString() + " " + unitName;
    }
}