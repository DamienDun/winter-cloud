package com.winter.cloud.datasource.constant;

/**
 * 数据源作业常量（执行器与 Handler 需与 XXL-JOB 控制台配置一致）
 */
public final class DsJobConstants {

    private DsJobConstants() {
    }

    /** XXL-JOB 执行器 AppName，统一使用 datasource-data-extra */
    public static final String EXECUTOR_APP_NAME = "datasource-data-extra";

    /** 执行器显示名 */
    public static final String EXECUTOR_TITLE = "数据源作业执行器";

    /** Bean 模式 JobHandler */
    public static final String EXECUTOR_HANDLER = "datasourceJobHandler";

    /** 作业状态 */
    public static final int JOB_STATUS_DISABLED = 0;
    public static final int JOB_STATUS_ENABLED = 1;
    public static final int JOB_STATUS_EXPIRED = 2;

    /** 最近执行状态 */
    public static final int LAST_EXEC_NONE = -1;
    public static final int LAST_EXEC_RUNNING = 0;
    public static final int LAST_EXEC_SUCCESS = 1;
    public static final int LAST_EXEC_FAIL = 2;
    public static final int LAST_EXEC_EXCEPTION = 3;

    /** 触发方式 */
    public static final int TRIGGER_MANUAL = 0;
    public static final int TRIGGER_CRON = 1;

    /** 同步方式 */
    public static final int SYNC_FULL = 0;
    public static final int SYNC_INCREMENT = 1;

    /** 日志执行状态 */
    public static final int LOG_RUNNING = 0;
    public static final int LOG_SUCCESS = 1;
    public static final int LOG_FAIL = 2;
}
