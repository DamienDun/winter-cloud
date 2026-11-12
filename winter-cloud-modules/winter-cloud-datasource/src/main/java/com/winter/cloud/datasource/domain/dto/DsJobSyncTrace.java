package com.winter.cloud.datasource.domain.dto;

import com.xxl.job.core.context.XxlJobHelper;

import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * 作业同步分步日志：写入 ds_job_log.job_message，并同步打到 XXL-JOB 执行日志。
 */
public class DsJobSyncTrace {

    /** 日志正文最大字符数，超出截断 */
    private static final int MAX_CHARS = 60000;
    /** 进度刷库最小间隔（毫秒），避免每一步都打库 */
    private static final long FLUSH_INTERVAL_MS = 1000L;

    /** 追踪开始时间戳（毫秒） */
    private final long startMs = System.currentTimeMillis();
    /** 分步日志缓冲 */
    private final StringBuilder sb = new StringBuilder(2048);
    /** 当前步骤序号 */
    private int stepNo;
    /** 是否同时镜像写入 XXL-JOB 执行日志（默认开启；非调度线程可关闭） */
    private final boolean mirrorToXxlJob;
    /** 终止检测回调：返回 true 表示任务已被请求终止 */
    private BooleanSupplier cancelChecker;
    /** 进度落库回调：接收当前完整日志文本 */
    private Consumer<String> progressSink;
    /** 上次刷进度时间戳（毫秒） */
    private long lastFlushMs;

    public DsJobSyncTrace() {
        this(true);
    }

    public DsJobSyncTrace(boolean mirrorToXxlJob) {
        this.mirrorToXxlJob = mirrorToXxlJob;
    }

    public void setCancelChecker(BooleanSupplier cancelChecker) {
        this.cancelChecker = cancelChecker;
    }

    public void setProgressSink(Consumer<String> progressSink) {
        this.progressSink = progressSink;
    }

    /** 协作式终止：批次间隙 / 步骤前调用 */
    public void checkCancel() {
        boolean cancelled = cancelChecker != null && cancelChecker.getAsBoolean();
        boolean interrupted = Thread.currentThread().isInterrupted();
        if (!cancelled && !interrupted) {
            return;
        }
        // 直接追加一行，避免再进 step()->checkCancel 递归
        appendRawLine(cancelled ? "检测到终止请求，停止同步" : "执行线程已中断，停止同步");
        flushProgress(true);
        throw new DsJobSyncException("任务已被手动终止", null, toString());
    }

    public void step(String format, Object... args) {
        checkCancel();
        String body;
        if (args == null || args.length == 0) {
            body = format;
        } else {
            body = String.format(Locale.ROOT, format, args);
        }
        appendRawLine(body);
        flushProgress(false);
    }

    private void appendRawLine(String body) {
        stepNo++;
        long elapsed = System.currentTimeMillis() - startMs;
        String line = String.format(Locale.ROOT, "[%d][+%dms] %s", stepNo, elapsed, body);
        sb.append(line).append('\n');
        mirrorXxl(line);
    }

    /** 强制把当前日志刷给监听方（结束时调用） */
    public void flushProgressNow() {
        flushProgress(true);
    }

    private void flushProgress(boolean force) {
        if (progressSink == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (!force && now - lastFlushMs < FLUSH_INTERVAL_MS) {
            return;
        }
        lastFlushMs = now;
        try {
            progressSink.accept(toString());
        } catch (Throwable ignore) {
            // 刷库失败不影响主流程
        }
    }

    public void fail(Throwable e) {
        if (e == null) {
            step("执行失败：未知异常");
            return;
        }
        step("执行失败：%s", e.getMessage() == null ? e.toString() : e.getMessage());
        Throwable cause = e.getCause();
        int depth = 0;
        while (cause != null && depth < 5) {
            step("  cause[%d]: %s", depth, cause.getMessage() == null ? cause.toString() : cause.getMessage());
            cause = cause.getCause();
            depth++;
        }
        StackTraceElement[] stack = e.getStackTrace();
        if (stack != null && stack.length > 0) {
            int limit = Math.min(8, stack.length);
            for (int i = 0; i < limit; i++) {
                step("  at %s", stack[i].toString());
            }
        }
    }

    public String summaryLine() {
        String all = sb.toString().trim();
        int nl = all.indexOf('\n');
        if (nl < 0) {
            return all;
        }
        String[] lines = all.split("\n");
        for (int i = lines.length - 1; i >= 0; i--) {
            String line = lines[i].trim();
            if (!line.isEmpty()) {
                return line;
            }
        }
        return all.substring(0, Math.min(200, all.length()));
    }

    @Override
    public String toString() {
        if (sb.length() <= MAX_CHARS) {
            return sb.toString();
        }
        return sb.substring(0, MAX_CHARS) + "\n... (日志过长已截断)\n";
    }

    /** 同步到 XXL-JOB 调度中心执行日志；无调度上下文时静默跳过 */
    private void mirrorXxl(String line) {
        if (!mirrorToXxlJob || line == null || line.isEmpty()) {
            return;
        }
        try {
            // 花括号在 XxlJobHelper.log 里是占位符，整行原样输出用 {}
            XxlJobHelper.log("{}", line);
        } catch (Throwable ignore) {
            // 非 XXL 执行线程调用时忽略
        }
    }
}
