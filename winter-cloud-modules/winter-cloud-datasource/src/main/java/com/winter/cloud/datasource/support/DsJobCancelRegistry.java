package com.winter.cloud.datasource.support;

import com.winter.cloud.datasource.domain.dto.DsJobSyncException;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Job cancel flags by ds_job_log.id, plus ThreadLocal bind for the running worker.
 * XXL kill interrupts the thread, but HTTP paging must poll this registry between pages.
 */
@Component
public class DsJobCancelRegistry {

    private final ConcurrentHashMap<Long, Boolean> flags = new ConcurrentHashMap<>();
    private final ThreadLocal<Long> currentJobLogId = new ThreadLocal<>();

    public void bind(Long jobLogId) {
        if (jobLogId != null) {
            currentJobLogId.set(jobLogId);
        }
    }

    public void unbind() {
        currentJobLogId.remove();
    }

    public void requestCancel(Long jobLogId) {
        if (jobLogId != null) {
            flags.put(jobLogId, Boolean.TRUE);
        }
    }

    public boolean isCancelRequested(Long jobLogId) {
        return jobLogId != null && Boolean.TRUE.equals(flags.get(jobLogId));
    }

    /** Whether current bound job was cancelled, or the thread was interrupted. */
    public boolean isCurrentCancelled() {
        Long id = currentJobLogId.get();
        return isCancelRequested(id) || Thread.currentThread().isInterrupted();
    }

    public void clear(Long jobLogId) {
        if (jobLogId != null) {
            flags.remove(jobLogId);
        }
    }

    /** Call between pages/batches; throws when cancel requested. */
    public void throwIfCancelled() {
        if (!isCurrentCancelled()) {
            return;
        }
        throw new DsJobSyncException("task cancelled by user", null, "task cancelled by user");
    }
}