package com.winter.cloud.datasource.domain.dto;

/**
 * 同步失败异常：携带分步详情日志，供作业日志落库
 */
public class DsJobSyncException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** 分步详情日志正文 */
    private final String detailLog;

    public DsJobSyncException(String message, Throwable cause, String detailLog) {
        super(message, cause);
        this.detailLog = detailLog;
    }

    public String getDetailLog() {
        return detailLog;
    }
}
