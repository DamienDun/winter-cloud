package com.winter.cloud.datasource.service.impl;

import com.winter.cloud.common.core.utils.StringUtils;
import com.winter.cloud.common.datasource.ddl.DsDdlDialect;
import com.winter.cloud.common.datasource.ddl.DsDdlDialectFactory;
import com.winter.cloud.common.datasource.ddl.model.DsColumnDef;
import com.winter.cloud.common.datasource.info.ColumnInfo;
import com.winter.cloud.common.datasource.query.AbstractDsQueryTool;
import com.winter.cloud.datasource.constant.DsJobConstants;
import com.winter.cloud.datasource.domain.dto.DsJobFieldMapping;
import com.winter.cloud.datasource.domain.dto.DsJobSyncException;
import com.winter.cloud.datasource.domain.dto.DsJobSyncResult;
import com.winter.cloud.datasource.domain.dto.DsJobSyncTrace;
import com.winter.cloud.datasource.domain.entity.DsApiDatasource;
import com.winter.cloud.datasource.domain.entity.DsJdbcDatasource;
import com.winter.cloud.datasource.domain.entity.DsJobInfo;
import com.winter.cloud.datasource.mapper.DsJdbcDatasourceMapper;
import com.winter.cloud.datasource.service.IDsApiDatasourceService;
import com.winter.cloud.datasource.service.IDsJobDataSyncService;
import com.winter.cloud.datasource.support.DsJobCancelRegistry;
import com.winter.cloud.datasource.support.DsQueryToolSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * 作业数据同步实现：API/JDBC 读源 → 目标 JDBC 写数；缺表建表、缺列补列、已有列按映射改类型/注释、已有表对齐主键
 */
@Service
public class DsJobDataSyncServiceImpl implements IDsJobDataSyncService {

    private static final Logger log = LoggerFactory.getLogger(DsJobDataSyncServiceImpl.class);

    private static final Pattern SAFE_IDENT = Pattern.compile("^[A-Za-z_][A-Za-z0-9_]*$");
    private static final int BATCH_SIZE = 1000;

    @Autowired
    private IDsApiDatasourceService apiDatasourceService;

    @Autowired
    private DsJdbcDatasourceMapper jdbcDatasourceMapper;

    @Autowired
    private DsQueryToolSupport queryToolSupport;

    @Autowired
    private DsJobCancelRegistry jobCancelRegistry;

    @Override
    public DsJobSyncResult sync(DsJobInfo job) {
        return sync(job, null, null);
    }

    @Override
    public DsJobSyncResult sync(DsJobInfo job, Long jobLogId, Consumer<String> progressSink) {
        DsJobSyncTrace trace = new DsJobSyncTrace();
        if (jobLogId != null) {
            jobCancelRegistry.bind(jobLogId);
            trace.setCancelChecker(() -> jobCancelRegistry.isCancelRequested(jobLogId)
                    || Thread.currentThread().isInterrupted());
        }
        if (progressSink != null) {
            trace.setProgressSink(progressSink);
        }
        DsJobSyncResult result = new DsJobSyncResult();
        try {
            DsJobSyncResult ok = doSync(job, result, trace);
            trace.flushProgressNow();
            return ok;
        } catch (DsJobSyncException e) {
            trace.flushProgressNow();
            throw e;
        } catch (RuntimeException e) {
            // doSync 的 finally 可能已写入部分步骤；再追加失败栈后抛出，供作业日志落库
            trace.fail(e);
            String detail = trace.toString();
            result.setDetailLog(detail);
            result.setMessage(e.getMessage() == null ? e.toString() : e.getMessage());
            trace.flushProgressNow();
            throw new DsJobSyncException(result.getMessage(), e, detail);
        } finally {
            jobCancelRegistry.unbind();
            if (jobLogId != null) {
                jobCancelRegistry.clear(jobLogId);
            }
        }
    }

    private DsJobSyncResult doSync(DsJobInfo job, DsJobSyncResult result, DsJobSyncTrace trace) {
        if (job == null) {
            throw new IllegalArgumentException("job required");
        }
        boolean apiSource = isApiSource(job.getDatasourceType());
        trace.step("开始同步：jobId=%s name=%s sourceType=%s syncType=%s",
                job.getId(),
                job.getJobName(),
                job.getDatasourceType(),
                Integer.valueOf(DsJobConstants.SYNC_FULL).equals(job.getSyncType()) ? "全量" : "增量");

        List<DsJobFieldMapping> mappings = requireMappings(job);
        trace.step("校验字段映射完成：mappingCount=%d", mappings.size());
        if (job.getTargetDatasourceId() == null || StringUtils.isEmpty(job.getTargetTable())) {
            throw new IllegalArgumentException("target datasource/table required");
        }
        validateIdent(job.getTargetTable(), "targetTable");
        for (DsJobFieldMapping m : mappings) {
            validateIdent(m.getTargetField(), "targetField");
        }

        // JDBC source: reuse column type/comment when create/add columns
        Map<String, SourceColumnMeta> sourceColumnMetas = loadSourceColumnMetas(job);
        if (apiSource) {
            trace.step("API 源：跳过源表列元数据加载");
        } else {
            trace.step("已加载源表列元数据：count=%d", sourceColumnMetas.size());
        }

        DsJdbcDatasource targetDs = requireJdbc(job.getTargetDatasourceId());
        DsDdlDialect dialect = DsDdlDialectFactory.get(targetDs.getDatasourceType());
        trace.step("目标数据源：id=%s name=%s type=%s table=%s",
                targetDs.getId(), targetDs.getDatasourceName(), targetDs.getDatasourceType(), job.getTargetTable());

        AbstractDsQueryTool tool = queryToolSupport.open(job.getTargetDatasourceId());
        try {
            if (apiSource) {
                syncFromApiByPage(job, result, trace, mappings, sourceColumnMetas, targetDs, dialect, tool);
            } else {
                List<Map<String, Object>> sourceRows = loadSourceRows(job, trace);
                result.setSourceCount(sourceRows.size());
                List<Map<String, Object>> targetRows = mapRows(sourceRows, mappings);
                trace.step("字段映射完成：sourceRows=%d → targetRows=%d", sourceRows.size(), targetRows.size());
                if (!sourceRows.isEmpty()) {
                    trace.step("源行样例字段：%s", previewKeys(sourceRows.get(0), 12));
                }
                if (!targetRows.isEmpty()) {
                    trace.step("目标行样例字段：%s", previewKeys(targetRows.get(0), 12));
                }
                EnsureSchemaOutcome schema = ensureTargetSchema(
                        tool, dialect, targetDs, job.getTargetTable(), mappings, targetRows,
                        sourceColumnMetas, false, trace);
                result.setTableCreated(schema.tableCreated);
                result.setColumnsAdded(schema.columnsAdded);
                result.setColumnsModified(schema.columnsModified);
                prepareTargetTable(job, tool, targetDs, trace);
                trace.step("开始写入目标表：rows=%d batchSize=%d", targetRows.size(), BATCH_SIZE);
                WriteStats stats = batchInsert(tool, targetDs.getDatasourceType(), job.getTargetTable(), mappings, targetRows, trace);
                result.setInsertedCount(stats.inserted);
                result.setErrorCount(stats.errors);
                finishSyncSummary(result, trace, stats, false);
            }
            return result;
        } finally {
            queryToolSupport.close(tool);
            trace.step("已关闭目标数据源连接");
            // 成功/失败都在此落盘详情；外层 catch 若再 append 失败栈会覆盖
            result.setDetailLog(trace.toString());
        }
    }

    /**
     * API 源同步：一页一拉、一页一写，避免全量累加占满内存。
     */
    private void syncFromApiByPage(DsJobInfo job,
                                   DsJobSyncResult result,
                                   DsJobSyncTrace trace,
                                   List<DsJobFieldMapping> mappings,
                                   Map<String, SourceColumnMeta> sourceColumnMetas,
                                   DsJdbcDatasource targetDs,
                                   DsDdlDialect dialect,
                                   AbstractDsQueryTool tool) {
        if (job.getDatasourceId() == null) {
            throw new IllegalArgumentException("api datasourceId required");
        }
        DsApiDatasource apiDs = apiDatasourceService.selectById(job.getDatasourceId());
        if (apiDs == null) {
            throw new IllegalArgumentException("api datasource not found");
        }
        boolean pageApi = Integer.valueOf(1).equals(apiDs.getPageApi());
        trace.step("拉取 API 源数据：apiDatasourceId=%s name=%s pageApi=%s",
                apiDs.getId(), apiDs.getDatasourceName(), pageApi ? "是" : "否");

        final boolean[] schemaReady = {false};
        final boolean[] tablePrepared = {false};
        final WriteStats totalStats = new WriteStats();
        final int[] sourceTotal = {0};
        final int[] pageCount = {0};

        apiDatasourceService.forEachDataPage(job.getDatasourceId(), (pageNum, pageRows) -> {
            if (pageRows == null || pageRows.isEmpty()) {
                trace.step("API 第 %d 页无数据，结束翻页", pageNum);
                return;
            }
            pageCount[0]++;
            sourceTotal[0] += pageRows.size();
            List<Map<String, Object>> targetRows = mapRows(pageRows, mappings);
            trace.step("API 第 %d 页：拉取 rows=%d → 映射 targetRows=%d", pageNum, pageRows.size(), targetRows.size());
            if (pageNum == 1) {
                trace.step("源行样例字段：%s", previewKeys(pageRows.get(0), 12));
                if (!targetRows.isEmpty()) {
                    trace.step("目标行样例字段：%s", previewKeys(targetRows.get(0), 12));
                }
            }

            if (!schemaReady[0]) {
                EnsureSchemaOutcome schema = ensureTargetSchema(
                        tool, dialect, targetDs, job.getTargetTable(), mappings, targetRows,
                        sourceColumnMetas, true, trace);
                result.setTableCreated(schema.tableCreated);
                result.setColumnsAdded(schema.columnsAdded);
                result.setColumnsModified(schema.columnsModified);
                schemaReady[0] = true;
            }
            if (!tablePrepared[0]) {
                prepareTargetTable(job, tool, targetDs, trace);
                tablePrepared[0] = true;
            }

            WriteStats pageStats = batchInsert(
                    tool, targetDs.getDatasourceType(), job.getTargetTable(), mappings, targetRows, trace);
            totalStats.inserted += pageStats.inserted;
            totalStats.errors += pageStats.errors;
            if (totalStats.firstError == null && pageStats.firstError != null) {
                totalStats.firstError = pageStats.firstError;
            }
            trace.step("API 第 %d 页写入完成：inserted=%d errors=%d 累计source=%d inserted=%d",
                    pageNum, pageStats.inserted, pageStats.errors, sourceTotal[0], totalStats.inserted);
        });

        result.setSourceCount(sourceTotal[0]);
        result.setInsertedCount(totalStats.inserted);
        result.setErrorCount(totalStats.errors);
        if (!schemaReady[0]) {
            // 全程无数据：仍尽量按映射建表，便于后续跑数
            EnsureSchemaOutcome schema = ensureTargetSchema(
                    tool, dialect, targetDs, job.getTargetTable(), mappings, new ArrayList<>(),
                    sourceColumnMetas, true, trace);
            result.setTableCreated(schema.tableCreated);
            result.setColumnsAdded(schema.columnsAdded);
            result.setColumnsModified(schema.columnsModified);
            prepareTargetTable(job, tool, targetDs, trace);
            trace.step("API 无业务数据可写入，已完成表结构对齐");
        }
        trace.step("API 同步翻页结束：pages=%d sourceRows=%d", pageCount[0], sourceTotal[0]);
        finishSyncSummary(result, trace, totalStats, true);
    }

    private void prepareTargetTable(DsJobInfo job, AbstractDsQueryTool tool, DsJdbcDatasource targetDs, DsJobSyncTrace trace) {
        if (Integer.valueOf(DsJobConstants.SYNC_FULL).equals(job.getSyncType())) {
            trace.step("全量同步：清空目标表 %s", job.getTargetTable());
            clearTargetTable(tool, targetDs.getDatasourceType(), job.getTargetTable());
            trace.step("目标表已清空");
        } else {
            trace.step("增量同步：不清空目标表，直接追加写入");
        }
    }

    private void finishSyncSummary(DsJobSyncResult result, DsJobSyncTrace trace, WriteStats stats, boolean apiSource) {
        String summary = String.format(
                Locale.ROOT,
                "sync done: from=%s sourceRows=%d inserted=%d errors=%d tableCreated=%s columnsAdded=%d columnsModified=%d",
                apiSource ? "api" : "jdbc",
                result.getSourceCount(),
                result.getInsertedCount(),
                result.getErrorCount(),
                result.isTableCreated(),
                result.getColumnsAdded(),
                result.getColumnsModified());
        result.setMessage(summary);
        trace.step("写入完成：inserted=%d errors=%d%s",
                stats.inserted,
                stats.errors,
                stats.firstError == null ? "" : (" firstError=" + stats.firstError));
        trace.step("同步结束：%s", summary);
        if (stats.errors > 0 && stats.inserted == 0) {
            throw new IllegalStateException(summary + "; firstError=" + stats.firstError);
        }
    }

    private String previewKeys(Map<String, Object> row, int limit) {
        if (row == null || row.isEmpty()) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        int i = 0;
        for (String k : row.keySet()) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(k);
            i++;
            if (i >= limit) {
                if (row.size() > limit) {
                    sb.append(", ...(+").append(row.size() - limit).append(")");
                }
                break;
            }
        }
        sb.append(']');
        return sb.toString();
    }

    private List<DsJobFieldMapping> requireMappings(DsJobInfo job) {
        List<DsJobFieldMapping> mappings = job.getFieldMappings();
        if (mappings == null || mappings.isEmpty()) {
            throw new IllegalArgumentException("fieldMappings required");
        }
        List<DsJobFieldMapping> cleaned = new ArrayList<>();
        Set<String> targetSeen = new LinkedHashSet<>();
        for (DsJobFieldMapping m : mappings) {
            if (m == null || StringUtils.isEmpty(m.getSourceField()) || StringUtils.isEmpty(m.getTargetField())) {
                continue;
            }
            String tf = m.getTargetField().trim();
            if (!targetSeen.add(tf)) {
                throw new IllegalArgumentException("duplicate targetField: " + tf);
            }
            DsJobFieldMapping copy = new DsJobFieldMapping();
            copy.setSourceField(m.getSourceField().trim());
            copy.setTargetField(tf);
            if (StringUtils.isNotEmpty(m.getTargetType())) {
                copy.setTargetType(m.getTargetType().trim());
            }
            if (StringUtils.isNotEmpty(m.getTargetComment())) {
                copy.setTargetComment(m.getTargetComment().trim());
            }
            // null 保留：旧作业未配置时建表仍可沿用源主键
            if (m.getPrimaryKey() != null) {
                copy.setPrimaryKey(m.getPrimaryKey());
            }
            cleaned.add(copy);
        }
        if (cleaned.isEmpty()) {
            throw new IllegalArgumentException("fieldMappings required");
        }
        return cleaned;
    }

    private List<Map<String, Object>> loadSourceRows(DsJobInfo job, DsJobSyncTrace trace) {
        // API 源：必须走 HTTP 拉取，禁止误走 JDBC
        if (isApiSource(job.getDatasourceType())) {
            if (job.getDatasourceId() == null) {
                throw new IllegalArgumentException("api datasourceId required");
            }
            trace.step("拉取 API 源数据：apiDatasourceId=%s name=%s",
                    job.getDatasourceId(), job.getDatasourceName());
            List<Map<String, Object>> rows = apiDatasourceService.fetchDataRows(job.getDatasourceId());
            int n = rows == null ? 0 : rows.size();
            log.info("api sync fetched rows={}, jobId={}, apiDatasourceId={}",
                    n, job.getId(), job.getDatasourceId());
            trace.step("API 拉取完成：rows=%d", n);
            return rows == null ? new ArrayList<>() : rows;
        }
        if (StringUtils.isEmpty(job.getSourceTable())) {
            throw new IllegalArgumentException("sourceTable required for jdbc source");
        }
        validateIdent(job.getSourceTable(), "sourceTable");
        trace.step("拉取 JDBC 源表：datasourceId=%s table=%s", job.getDatasourceId(), job.getSourceTable());
        List<Map<String, Object>> rows = loadJdbcSourceRows(job.getDatasourceId(), job.getSourceTable());
        int n = rows == null ? 0 : rows.size();
        log.info("jdbc sync fetched rows={}, jobId={}, table={}",
                n, job.getId(), job.getSourceTable());
        trace.step("JDBC 拉取完成：rows=%d", n);
        return rows;
    }

    private List<Map<String, Object>> loadJdbcSourceRows(Long datasourceId, String tableName) {
        DsJdbcDatasource ds = requireJdbc(datasourceId);
        AbstractDsQueryTool tool = queryToolSupport.open(datasourceId);
        try {
            String qTable = DsQueryToolSupport.quoteIdent(ds.getDatasourceType(), tableName);
            String sql = "SELECT * FROM " + qTable;
            List<Map<String, Object>> rows = tool.getJdbcTemplate().queryForList(sql);
            // 统一为大小写不敏感查找：额外放入小写 key
            List<Map<String, Object>> normalized = new ArrayList<>(rows.size());
            for (Map<String, Object> row : rows) {
                Map<String, Object> map = new LinkedHashMap<>();
                if (row != null) {
                    for (Map.Entry<String, Object> e : row.entrySet()) {
                        if (e.getKey() == null) {
                            continue;
                        }
                        map.put(e.getKey(), e.getValue());
                        map.putIfAbsent(e.getKey().toLowerCase(Locale.ROOT), e.getValue());
                    }
                }
                normalized.add(map);
            }
            return normalized;
        } finally {
            queryToolSupport.close(tool);
        }
    }

    private List<Map<String, Object>> mapRows(List<Map<String, Object>> sourceRows, List<DsJobFieldMapping> mappings) {
        List<Map<String, Object>> out = new ArrayList<>(sourceRows.size());
        for (Map<String, Object> source : sourceRows) {
            Map<String, Object> target = new LinkedHashMap<>();
            for (DsJobFieldMapping m : mappings) {
                target.put(m.getTargetField(), readSourceValue(source, m.getSourceField()));
            }
            out.add(target);
        }
        return out;
    }

    private Object readSourceValue(Map<String, Object> source, String sourceField) {
        if (source == null || StringUtils.isEmpty(sourceField)) {
            return null;
        }
        if (source.containsKey(sourceField)) {
            return source.get(sourceField);
        }
        String lower = sourceField.toLowerCase(Locale.ROOT);
        if (source.containsKey(lower)) {
            return source.get(lower);
        }
        // data.chiefNo → chiefNo
        int dot = sourceField.lastIndexOf('.');
        if (dot >= 0 && dot < sourceField.length() - 1) {
            String leaf = sourceField.substring(dot + 1);
            if (source.containsKey(leaf)) {
                return source.get(leaf);
            }
            String leafLower = leaf.toLowerCase(Locale.ROOT);
            if (source.containsKey(leafLower)) {
                return source.get(leafLower);
            }
        }
        return null;
    }

    private Map<String, SourceColumnMeta> loadSourceColumnMetas(DsJobInfo job) {
        Map<String, SourceColumnMeta> map = new LinkedHashMap<>();
        String type = job.getDatasourceType() == null ? "" : job.getDatasourceType().trim().toLowerCase(Locale.ROOT);
        if ("api".equals(type) || StringUtils.isEmpty(job.getSourceTable()) || job.getDatasourceId() == null) {
            return map;
        }
        DsJdbcDatasource sourceDs = requireJdbc(job.getDatasourceId());
        AbstractDsQueryTool tool = queryToolSupport.open(job.getDatasourceId());
        try {
            String schema = resolveSchemaName(sourceDs, tool);
            // Prefer information_schema with explicit schema (never use datasourceName)
            List<Map<String, Object>> rows = tool.getJdbcTemplate().queryForList(
                    "SELECT COLUMN_NAME AS name, COLUMN_TYPE AS colType, "
                            + "IFNULL(COLUMN_COMMENT,'') AS colComment, "
                            + "IS_NULLABLE AS nullable, COLUMN_KEY AS colKey, "
                            + "ORDINAL_POSITION AS ordPos "
                            + "FROM information_schema.COLUMNS "
                            + "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? "
                            + "ORDER BY ORDINAL_POSITION",
                    schema, job.getSourceTable());
            if (rows != null && !rows.isEmpty()) {
                int fallbackOrd = 1;
                for (Map<String, Object> row : rows) {
                    SourceColumnMeta meta = new SourceColumnMeta();
                    meta.name = String.valueOf(row.get("name"));
                    meta.columnType = row.get("colType") == null ? null : String.valueOf(row.get("colType"));
                    meta.comment = row.get("colComment") == null ? null : String.valueOf(row.get("colComment"));
                    meta.nullable = !"NO".equalsIgnoreCase(String.valueOf(row.get("nullable")));
                    meta.primaryKey = "PRI".equalsIgnoreCase(String.valueOf(row.get("colKey")));
                    meta.ordinalPosition = toOrd(row.get("ordPos"), fallbackOrd++);
                    if (StringUtils.isNotEmpty(meta.name)) {
                        map.put(meta.name.toLowerCase(Locale.ROOT), meta);
                    }
                }
                return map;
            }
            int ord = 1;
            for (ColumnInfo col : tool.getColumnInfos(job.getSourceTable())) {
                if (col == null || StringUtils.isEmpty(col.getName())) {
                    continue;
                }
                SourceColumnMeta meta = new SourceColumnMeta();
                meta.name = col.getName();
                meta.columnType = normalizeRawType(col.getType());
                meta.comment = col.getComment();
                meta.nullable = col.getAllowNull() != 0;
                meta.primaryKey = col.isPrimarykey();
                meta.ordinalPosition = ord++;
                map.put(meta.name.toLowerCase(Locale.ROOT), meta);
            }
            return map;
        } catch (Exception e) {
            log.warn("load source column meta failed, fallback to sample inference: {}", e.getMessage());
            return map;
        } finally {
            queryToolSupport.close(tool);
        }
    }

    private EnsureSchemaOutcome ensureTargetSchema(AbstractDsQueryTool tool,
                                                   DsDdlDialect dialect,
                                                   DsJdbcDatasource targetDs,
                                                   String tableName,
                                                   List<DsJobFieldMapping> mappings,
                                                   List<Map<String, Object>> targetRows,
                                                   Map<String, SourceColumnMeta> sourceColumnMetas,
                                                   boolean apiSource,
                                                   DsJobSyncTrace trace) {
        EnsureSchemaOutcome outcome = new EnsureSchemaOutcome();
        String schema = resolveSchemaName(targetDs, tool);
        boolean exists = tableExists(tool, tableName);
        Map<String, Object> typeSample = buildTypeSample(mappings, targetRows);
        // 映射列表顺序 = 建表/补列顺序（前端可调）；无源序元数据时保持原列表
        List<DsJobFieldMapping> orderedMappings = mappings;
        boolean mysqlLike = isMysqlLike(targetDs.getDatasourceType());
        trace.step("检查目标表结构：schema=%s table=%s exists=%s", schema, tableName, exists);
        if (!exists) {
            List<DsColumnDef> columns = new ArrayList<>();
            for (DsJobFieldMapping m : orderedMappings) {
                columns.add(buildColumnDef(m, typeSample.get(m.getTargetField()), sourceColumnMetas, apiSource));
            }
            trace.step("目标表不存在，开始建表：columnCount=%d", columns.size());
            for (String sql : dialect.buildCreateTableSqls(tableName, "synced by datasource job", columns)) {
                trace.step("执行建表 SQL：%s", abbreviate(sql, 300));
                tool.getJdbcTemplate().execute(sql);
            }
            outcome.tableCreated = true;
            outcome.columnsAdded = columns.size();
            log.info("auto create target table {}.{}.{}", schema, tableName, targetDs.getDatasourceName());
            trace.step("建表完成：%s.%s columns=%d", schema, tableName, columns.size());
            return outcome;
        }

        Map<String, ExistingColumnMeta> existingMetas = listExistingColumnMetas(tool, schema, tableName);
        trace.step("目标表已存在，现有列数=%d，开始按映射对齐", existingMetas.size());
        String lastPresentTarget = null;
        for (DsJobFieldMapping m : orderedMappings) {
            String tf = m.getTargetField();
            String tfKey = tf.toLowerCase(Locale.ROOT);
            ExistingColumnMeta exist = existingMetas.get(tfKey);
            if (exist != null) {
                lastPresentTarget = exist.name;
                // 已有列：映射里显式改了类型/注释则 MODIFY
                if (tryModifyExistingColumn(tool, dialect, schema, tableName, m, exist,
                        typeSample.get(tf), sourceColumnMetas, apiSource, trace)) {
                    outcome.columnsModified++;
                }
                continue;
            }
            DsColumnDef col = buildColumnDef(m, typeSample.get(tf), sourceColumnMetas, apiSource);
            // MySQL: ADD ... AFTER 前一列，使补列位置贴近映射顺序
            if (mysqlLike && StringUtils.isNotEmpty(lastPresentTarget)) {
                col.setAfterColumn(lastPresentTarget);
            }
            try {
                for (String sql : dialect.buildAlterColumnSqls(tableName, "ADD", col)) {
                    trace.step("新增列 %s type=%s comment=%s after=%s；SQL=%s",
                            tf, col.getType(), col.getComment(), col.getAfterColumn(), abbreviate(sql, 240));
                    tool.getJdbcTemplate().execute(sql);
                }
                ExistingColumnMeta added = new ExistingColumnMeta();
                added.name = tf;
                added.columnType = col.getType();
                added.comment = col.getComment() == null ? "" : col.getComment();
                added.nullable = !Boolean.FALSE.equals(col.getNullable());
                existingMetas.put(tfKey, added);
                outcome.columnsAdded++;
                lastPresentTarget = tf;
                log.info("auto add column {}.{}.{} type={} comment={} after={}",
                        schema, tableName, tf, col.getType(), col.getComment(), col.getAfterColumn());
            } catch (Exception e) {
                String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase(Locale.ROOT);
                // concurrent / already-exists: treat as present and continue
                if (msg.contains("duplicate column") || msg.contains("already exists")) {
                    ExistingColumnMeta skipped = new ExistingColumnMeta();
                    skipped.name = tf;
                    skipped.columnType = col.getType();
                    skipped.comment = col.getComment() == null ? "" : col.getComment();
                    skipped.nullable = true;
                    existingMetas.put(tfKey, skipped);
                    lastPresentTarget = tf;
                    log.warn("skip add column {}.{}.{}: {}", schema, tableName, tf, e.getMessage());
                    trace.step("跳过新增列 %s（已存在）：%s", tf, e.getMessage());
                    continue;
                }
                throw e;
            }
        }
        // 列对齐后再同步主键（建表路径已在 CREATE 时写入；此处覆盖「表已存在」场景）
        if (syncPrimaryKey(tool, dialect, schema, tableName, orderedMappings, existingMetas, trace)) {
            outcome.primaryKeySynced = true;
        }
        trace.step("表结构对齐完成：added=%d modified=%d pkSynced=%s",
                outcome.columnsAdded, outcome.columnsModified, outcome.primaryKeySynced);
        return outcome;
    }

    /**
     * 按映射勾选的主键，对齐已有目标表 PRIMARY KEY。
     * @return true 表示执行了 DROP/ADD 主键
     */
    private boolean syncPrimaryKey(AbstractDsQueryTool tool,
                                   DsDdlDialect dialect,
                                   String schema,
                                   String tableName,
                                   List<DsJobFieldMapping> mappings,
                                   Map<String, ExistingColumnMeta> existingMetas,
                                   DsJobSyncTrace trace) {
        List<String> desiredPk = new ArrayList<>();
        for (DsJobFieldMapping m : mappings) {
            if (m == null || !Boolean.TRUE.equals(m.getPrimaryKey()) || StringUtils.isEmpty(m.getTargetField())) {
                continue;
            }
            ExistingColumnMeta exist = existingMetas.get(m.getTargetField().trim().toLowerCase(Locale.ROOT));
            if (exist == null || StringUtils.isEmpty(exist.name)) {
                throw new IllegalArgumentException("主键列在目标表中不存在: " + m.getTargetField());
            }
            desiredPk.add(exist.name);
        }
        if (desiredPk.isEmpty()) {
            trace.step("映射未勾选主键，跳过主键对齐");
            return false;
        }

        List<String> existingPk = listExistingPrimaryKeyColumns(tool, dialect, schema, tableName);
        if (samePrimaryKey(existingPk, desiredPk)) {
            trace.step("主键已一致：%s", String.join(",", desiredPk));
            return false;
        }

        List<String> alterPkSqls = dialect.buildAlterPrimaryKeySqls(tableName, !existingPk.isEmpty(), desiredPk);
        if (alterPkSqls == null || alterPkSqls.isEmpty()) {
            trace.step("当前目标库方言不支持已有表调整主键（dialect=%s），期望主键=%s",
                    dialect.name(), String.join(",", desiredPk));
            return false;
        }

        // ADD PRIMARY KEY 前：主键列必须 NOT NULL
        for (String pkName : desiredPk) {
            ExistingColumnMeta exist = existingMetas.get(pkName.toLowerCase(Locale.ROOT));
            if (exist == null || !exist.nullable) {
                continue;
            }
            DsColumnDef col = new DsColumnDef();
            col.setName(exist.name);
            col.setType(StringUtils.isNotEmpty(exist.columnType) ? exist.columnType : "varchar(255)");
            col.setComment(exist.comment);
            col.setNullable(false);
            for (String sql : dialect.buildAlterColumnSqls(tableName, "MODIFY", col)) {
                trace.step("主键列改 NOT NULL：%s；SQL=%s", exist.name, abbreviate(sql, 240));
                tool.getJdbcTemplate().execute(sql);
            }
            exist.nullable = false;
        }

        for (String sql : alterPkSqls) {
            trace.step("对齐主键 existing=[%s] desired=[%s]；SQL=%s",
                    String.join(",", existingPk), String.join(",", desiredPk), abbreviate(sql, 300));
            tool.getJdbcTemplate().execute(sql);
        }
        log.info("sync primary key {}.{} -> [{}]", schema, tableName, String.join(",", desiredPk));
        return true;
    }

    @SuppressWarnings("unchecked")
    private List<String> listExistingPrimaryKeyColumns(AbstractDsQueryTool tool,
                                                       DsDdlDialect dialect,
                                                       String schema,
                                                       String tableName) {
        // 优先 information_schema（带 schema，顺序稳定）
        try {
            List<Map<String, Object>> rows = tool.getJdbcTemplate().queryForList(
                    "SELECT COLUMN_NAME AS name FROM information_schema.KEY_COLUMN_USAGE "
                            + "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? AND CONSTRAINT_NAME = 'PRIMARY' "
                            + "ORDER BY ORDINAL_POSITION",
                    schema, tableName);
            List<String> cols = new ArrayList<>();
            if (rows != null) {
                for (Map<String, Object> row : rows) {
                    Object name = row.get("name");
                    if (name != null && StringUtils.isNotEmpty(String.valueOf(name))) {
                        cols.add(String.valueOf(name));
                    }
                }
            }
            return cols;
        } catch (Exception e) {
            log.warn("list primary key via information_schema failed, fallback listIndexes: {}", e.getMessage());
        }
        try {
            List<Map<String, Object>> indexes = dialect.listIndexes(tool.getJdbcTemplate(), tableName);
            if (indexes != null) {
                for (Map<String, Object> idx : indexes) {
                    if (idx == null) {
                        continue;
                    }
                    String indexType = String.valueOf(idx.get("indexType"));
                    String indexName = String.valueOf(idx.get("indexName"));
                    if (!"PRIMARY".equalsIgnoreCase(indexType) && !"PRIMARY".equalsIgnoreCase(indexName)) {
                        continue;
                    }
                    Object columns = idx.get("columns");
                    if (columns instanceof List) {
                        List<String> cols = new ArrayList<>();
                        for (Object c : (List<Object>) columns) {
                            if (c != null && StringUtils.isNotEmpty(String.valueOf(c))) {
                                cols.add(String.valueOf(c));
                            }
                        }
                        return cols;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("list primary key via listIndexes failed: {}", e.getMessage());
        }
        return Collections.emptyList();
    }

    private boolean samePrimaryKey(List<String> existing, List<String> desired) {
        if (existing == null || desired == null || existing.size() != desired.size()) {
            return false;
        }
        for (int i = 0; i < existing.size(); i++) {
            if (!existing.get(i).equalsIgnoreCase(desired.get(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * 对比映射配置与目标已有列；类型或注释有差异则执行 MODIFY。
     * @return true 表示已执行修改
     */
    private boolean tryModifyExistingColumn(AbstractDsQueryTool tool,
                                            DsDdlDialect dialect,
                                            String schema,
                                            String tableName,
                                            DsJobFieldMapping mapping,
                                            ExistingColumnMeta exist,
                                            Object sampleValue,
                                            Map<String, SourceColumnMeta> sourceColumnMetas,
                                            boolean apiSource,
                                            DsJobSyncTrace trace) {
        boolean typeConfigured = StringUtils.isNotEmpty(mapping.getTargetType());
        boolean commentConfigured = StringUtils.isNotEmpty(mapping.getTargetComment());
        if (!typeConfigured && !commentConfigured) {
            return false;
        }

        DsColumnDef desired = buildColumnDef(mapping, sampleValue, sourceColumnMetas, apiSource);
        // 改类型时保留目标列可空性，避免误加/误删 NOT NULL
        desired.setNullable(exist.nullable);
        desired.setPrimaryKey(false);

        String existType = normalizeRawType(exist.columnType);
        String desiredType = normalizeRawType(desired.getType());
        String existComment = exist.comment == null ? "" : exist.comment;
        String desiredComment = desired.getComment() == null ? "" : desired.getComment();

        boolean typeDiff = typeConfigured && desiredType != null && !sameSqlType(desiredType, existType);
        boolean commentDiff = commentConfigured && !desiredComment.equals(existComment);
        if (!typeDiff && !commentDiff) {
            return false;
        }
        // 只改注释时沿用库中类型；只改类型时沿用库中注释（若映射未配注释）
        if (!typeDiff) {
            desired.setType(existType != null ? existType : exist.columnType);
        }
        if (!commentDiff) {
            desired.setComment(existComment);
        }

        try {
            for (String sql : dialect.buildAlterColumnSqls(tableName, "MODIFY", desired)) {
                trace.step("修改列 %s：type %s -> %s，comment='%s'；SQL=%s",
                        exist.name, existType, desired.getType(), desired.getComment(), abbreviate(sql, 240));
                tool.getJdbcTemplate().execute(sql);
            }
            exist.columnType = desired.getType();
            exist.comment = desired.getComment() == null ? "" : desired.getComment();
            log.info("auto modify column {}.{}.{} type {} -> {}, comment='{}'",
                    schema, tableName, exist.name, existType, desired.getType(), exist.comment);
            return true;
        } catch (IllegalArgumentException e) {
            // 部分方言不支持 MODIFY（如 Presto）
            log.warn("skip modify column {}.{}.{}: {}", schema, tableName, exist.name, e.getMessage());
            trace.step("跳过修改列 %s：%s", exist.name, e.getMessage());
            return false;
        }
    }

    private String abbreviate(String text, int max) {
        if (text == null) {
            return "";
        }
        String oneLine = text.replace('\n', ' ').replaceAll("\\s+", " ").trim();
        if (oneLine.length() <= max) {
            return oneLine;
        }
        return oneLine.substring(0, max) + "...";
    }

    private boolean sameSqlType(String a, String b) {
        String na = normalizeRawType(a);
        String nb = normalizeRawType(b);
        if (na == null || nb == null) {
            return false;
        }
        return na.equals(nb);
    }

    private boolean isMysqlLike(String datasourceType) {
        String t = datasourceType == null ? "" : datasourceType.toLowerCase(Locale.ROOT);
        return t.contains("mysql") || t.contains("mariadb");
    }

    private int toOrd(Object v, int fallback) {
        if (v instanceof Number) {
            return ((Number) v).intValue();
        }
        if (v != null) {
            try {
                return Integer.parseInt(String.valueOf(v));
            } catch (Exception ignore) {
                // fall through
            }
        }
        return fallback;
    }

    /**
     * Resolve real DB schema: databaseName field > jdbcUrl path > SELECT DATABASE().
     * Do NOT use datasourceName (e.g. 本地ruonan库).
     */
    private String resolveSchemaName(DsJdbcDatasource ds, AbstractDsQueryTool tool) {
        if (ds != null && StringUtils.isNotEmpty(ds.getDatabaseName())) {
            return ds.getDatabaseName().trim();
        }
        String fromUrl = parseDatabaseFromJdbcUrl(ds == null ? null : ds.getJdbcUrl());
        if (StringUtils.isNotEmpty(fromUrl)) {
            return fromUrl;
        }
        try {
            String db = tool.getJdbcTemplate().queryForObject("SELECT DATABASE()", String.class);
            if (StringUtils.isNotEmpty(db)) {
                return db;
            }
        } catch (Exception e) {
            log.warn("SELECT DATABASE() failed: {}", e.getMessage());
        }
        throw new IllegalStateException("cannot resolve database schema for datasourceId="
                + (ds == null ? null : ds.getId()));
    }

    private String parseDatabaseFromJdbcUrl(String jdbcUrl) {
        if (StringUtils.isEmpty(jdbcUrl)) {
            return null;
        }
        // jdbc:mysql://host:port/dbname?params
        int scheme = jdbcUrl.indexOf("://");
        if (scheme < 0) {
            return null;
        }
        String rest = jdbcUrl.substring(scheme + 3);
        int slash = rest.indexOf('/');
        if (slash < 0 || slash == rest.length() - 1) {
            return null;
        }
        String dbAndParams = rest.substring(slash + 1);
        int q = dbAndParams.indexOf('?');
        String db = q >= 0 ? dbAndParams.substring(0, q) : dbAndParams;
        db = db.trim();
        return db.isEmpty() ? null : db;
    }

    /** Prefer information_schema with explicit schema name（含类型/注释，供 MODIFY 对比）. */
    private Map<String, ExistingColumnMeta> listExistingColumnMetas(AbstractDsQueryTool tool,
                                                                    String schema,
                                                                    String tableName) {
        Map<String, ExistingColumnMeta> existing = new LinkedHashMap<>();
        try {
            List<Map<String, Object>> rows = tool.getJdbcTemplate().queryForList(
                    "SELECT COLUMN_NAME AS name, COLUMN_TYPE AS columnType, "
                            + "IFNULL(COLUMN_COMMENT,'') AS columnComment, IS_NULLABLE AS isNullable "
                            + "FROM information_schema.COLUMNS "
                            + "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? "
                            + "ORDER BY ORDINAL_POSITION",
                    schema, tableName);
            if (rows != null) {
                for (Map<String, Object> row : rows) {
                    Object name = row.get("name");
                    if (name == null) {
                        continue;
                    }
                    ExistingColumnMeta meta = new ExistingColumnMeta();
                    meta.name = String.valueOf(name);
                    meta.columnType = row.get("columnType") == null ? "" : String.valueOf(row.get("columnType"));
                    meta.comment = row.get("columnComment") == null ? "" : String.valueOf(row.get("columnComment"));
                    meta.nullable = !"NO".equalsIgnoreCase(String.valueOf(row.get("isNullable")));
                    existing.put(meta.name.toLowerCase(Locale.ROOT), meta);
                }
            }
            log.info("target schema={}, table={}, existingColumns={}", schema, tableName, existing.size());
        } catch (Exception e) {
            log.warn("list columns via information_schema failed, fallback getColumnInfos: {}", e.getMessage());
        }
        if (!existing.isEmpty()) {
            return existing;
        }
        for (ColumnInfo col : tool.getColumnInfos(tableName)) {
            if (col == null || StringUtils.isEmpty(col.getName())) {
                continue;
            }
            ExistingColumnMeta meta = new ExistingColumnMeta();
            meta.name = col.getName();
            meta.columnType = col.getType() == null ? "" : col.getType();
            meta.comment = col.getComment() == null ? "" : col.getComment();
            meta.nullable = col.getAllowNull() != 0;
            existing.put(meta.name.toLowerCase(Locale.ROOT), meta);
        }
        return existing;
    }

    private Map<String, Object> buildTypeSample(List<DsJobFieldMapping> mappings, List<Map<String, Object>> targetRows) {
        Map<String, Object> sample = new LinkedHashMap<>();
        for (DsJobFieldMapping m : mappings) {
            sample.put(m.getTargetField(), null);
        }
        for (Map<String, Object> row : targetRows) {
            for (DsJobFieldMapping m : mappings) {
                String tf = m.getTargetField();
                if (sample.get(tf) != null) {
                    continue;
                }
                Object v = row.get(tf);
                if (v != null) {
                    sample.put(tf, v);
                }
            }
        }
        return sample;
    }

    private DsColumnDef buildColumnDef(DsJobFieldMapping mapping,
                                       Object sampleValue,
                                       Map<String, SourceColumnMeta> sourceColumnMetas,
                                       boolean apiSource) {
        DsColumnDef col = new DsColumnDef();
        col.setName(mapping.getTargetField());
        SourceColumnMeta meta = findSourceMeta(mapping.getSourceField(), sourceColumnMetas);

        // 主键：映射显式配置优先；未配置时 JDBC 沿用源列 PRI（兼容旧作业）
        boolean primaryKey = mapping.getPrimaryKey() != null
                ? Boolean.TRUE.equals(mapping.getPrimaryKey())
                : (meta != null && meta.primaryKey);
        col.setPrimaryKey(primaryKey);
        if (primaryKey) {
            col.setNullable(false);
        } else if (meta != null) {
            col.setNullable(meta.nullable);
        } else {
            col.setNullable(true);
        }

        // 类型：映射配置 > 源列类型 > 样例值推断（API/JDBC 均适用）
        if (StringUtils.isNotEmpty(mapping.getTargetType())) {
            col.setType(normalizeRawType(mapping.getTargetType()));
        } else if (meta != null && StringUtils.isNotEmpty(meta.columnType)) {
            col.setType(normalizeRawType(meta.columnType));
        } else {
            col.setType(inferSqlType(sampleValue));
        }
        // 主键列不能落成 TEXT/BLOB；普通列过宽 VARCHAR 降为 TEXT 控行宽
        if (primaryKey) {
            col.setType(fitPrimaryKeyMysqlType(col.getType()));
        } else {
            col.setType(fitMysqlRowSizeType(col.getType()));
        }

        // 注释：映射配置 > 源注释 > 源字段名
        if (StringUtils.isNotEmpty(mapping.getTargetComment())) {
            col.setComment(mapping.getTargetComment());
        } else if (meta != null && StringUtils.isNotEmpty(meta.comment)) {
            col.setComment(meta.comment);
        } else if (StringUtils.isNotEmpty(mapping.getSourceField())) {
            col.setComment(mapping.getSourceField());
        }
        return col;
    }

    private boolean isApiSource(String datasourceType) {
        return datasourceType != null && "api".equalsIgnoreCase(datasourceType.trim());
    }

    private SourceColumnMeta findSourceMeta(String sourceField, Map<String, SourceColumnMeta> metas) {
        if (metas == null || metas.isEmpty() || StringUtils.isEmpty(sourceField)) {
            return null;
        }
        SourceColumnMeta meta = metas.get(sourceField.toLowerCase(Locale.ROOT));
        if (meta != null) {
            return meta;
        }
        int dot = sourceField.lastIndexOf('.');
        if (dot >= 0 && dot < sourceField.length() - 1) {
            return metas.get(sourceField.substring(dot + 1).toLowerCase(Locale.ROOT));
        }
        return null;
    }

    private String normalizeRawType(String raw) {
        if (StringUtils.isEmpty(raw)) {
            return null;
        }
        String t = raw.trim().toLowerCase(Locale.ROOT).replace(" ", "");
        // bit(1) -> tinyint(1) for broader dialect compatibility
        if (t.startsWith("bit")) {
            return "tinyint(1)";
        }
        // MySQL 显示宽度：bigint(20)/int(11) 与 bigint/int 视为同类；tinyint(1) 保留
        if (t.matches("^(tinyint|smallint|mediumint|int|integer|bigint)\\(\\d+\\)$")) {
            if (t.startsWith("tinyint(1)")) {
                return "tinyint(1)";
            }
            t = t.replaceAll("\\(\\d+\\)", "");
        }
        if ("integer".equals(t)) {
            return "int";
        }
        return t;
    }

    private String inferSqlType(Object value) {
        if (value == null) {
            return "varchar(255)";
        }
        if (value instanceof Boolean) {
            return "tinyint(1)";
        }
        if (value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long) {
            return "bigint";
        }
        // 小数用 varchar，避免 decimal/浮点精度丢失（尤其 API JSON 数字）
        if (value instanceof Float || value instanceof Double || value instanceof BigDecimal) {
            return varcharBySampleLength(String.valueOf(value));
        }
        if (value instanceof Date || value instanceof java.sql.Timestamp || value instanceof java.sql.Date
                || value instanceof java.time.LocalDateTime || value instanceof java.time.LocalDate) {
            return "datetime";
        }
        String s = String.valueOf(value).trim();
        // common datetime string from JDBC/API
        if (s.matches("^\\d{4}-\\d{2}-\\d{2}([ T]\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?)?$")) {
            return "datetime";
        }
        if (s.matches("^-?\\d+\\.\\d+([eE][+-]?\\d+)?$")) {
            return varcharBySampleLength(s);
        }
        return varcharBySampleLength(s);
    }

    private String varcharBySampleLength(String s) {
        int len = s == null ? 0 : s.trim().length();
        // 短字段用 VARCHAR；再长直接 TEXT，避免多列宽 VARCHAR 触发行宽上限
        if (len <= 64) {
            return "varchar(128)";
        }
        if (len <= 191) {
            return "varchar(255)";
        }
        return "text";
    }

    /**
     * MySQL/InnoDB：utf8mb4 下 varchar(N) 按最多 4N 字节计入行宽；超过 255 的 VARCHAR 统一降为 TEXT。
     */
    private String fitMysqlRowSizeType(String type) {
        if (StringUtils.isEmpty(type)) {
            return "varchar(255)";
        }
        String t = type.trim().toLowerCase(Locale.ROOT).replace(" ", "");
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("^varchar\\((\\d+)\\)$").matcher(t);
        if (m.matches()) {
            int n = Integer.parseInt(m.group(1));
            if (n > 255) {
                return "text";
            }
        }
        m = java.util.regex.Pattern.compile("^char\\((\\d+)\\)$").matcher(t);
        if (m.matches()) {
            int n = Integer.parseInt(m.group(1));
            if (n > 255) {
                return "text";
            }
        }
        return type;
    }

    /**
     * 主键列：禁止 TEXT/BLOB/JSON；过宽 VARCHAR/CHAR 收窄到 255，避免建表失败。
     */
    private String fitPrimaryKeyMysqlType(String type) {
        if (StringUtils.isEmpty(type)) {
            return "varchar(255)";
        }
        String t = type.trim().toLowerCase(Locale.ROOT).replace(" ", "");
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("^varchar\\((\\d+)\\)$").matcher(t);
        if (m.matches()) {
            int n = Integer.parseInt(m.group(1));
            return "varchar(" + Math.min(n, 255) + ")";
        }
        m = java.util.regex.Pattern.compile("^char\\((\\d+)\\)$").matcher(t);
        if (m.matches()) {
            int n = Integer.parseInt(m.group(1));
            return n > 255 ? "varchar(255)" : t;
        }
        if ("text".equals(t) || "tinytext".equals(t) || "mediumtext".equals(t) || "longtext".equals(t)
                || "json".equals(t) || t.contains("blob")) {
            return "varchar(255)";
        }
        return t;
    }

    private void clearTargetTable(AbstractDsQueryTool tool, String dsType, String tableName) {
        String qTable = DsQueryToolSupport.quoteIdent(dsType, tableName);
        tool.getJdbcTemplate().update("DELETE FROM " + qTable);
    }

    private WriteStats batchInsert(AbstractDsQueryTool tool,
                                   String dsType,
                                   String tableName,
                                   List<DsJobFieldMapping> mappings,
                                   List<Map<String, Object>> targetRows,
                                   DsJobSyncTrace trace) {
        WriteStats stats = new WriteStats();
        if (targetRows.isEmpty()) {
            trace.step("无数据可写入，跳过 INSERT");
            return stats;
        }
        List<String> cols = new ArrayList<>();
        for (DsJobFieldMapping m : mappings) {
            cols.add(DsQueryToolSupport.quoteIdent(dsType, m.getTargetField()));
        }
        String qTable = DsQueryToolSupport.quoteIdent(dsType, tableName);
        String placeholders = String.join(",", Collections.nCopies(cols.size(), "?"));
        String sql = "INSERT INTO " + qTable + " (" + String.join(",", cols) + ") VALUES (" + placeholders + ")";
        trace.step("准备 INSERT：columns=%d sql=%s", cols.size(), abbreviate(sql, 280));
        JdbcTemplate jdbc = tool.getJdbcTemplate();

        List<Object[]> batch = new ArrayList<>(BATCH_SIZE);
        int batchNo = 0;
        for (Map<String, Object> row : targetRows) {
            Object[] args = new Object[mappings.size()];
            for (int i = 0; i < mappings.size(); i++) {
                args[i] = normalizeJdbcValue(row.get(mappings.get(i).getTargetField()));
            }
            batch.add(args);
            if (batch.size() >= BATCH_SIZE) {
                // 每批写入前检查终止，避免大任务杀不掉
                trace.checkCancel();
                batchNo++;
                flushBatch(jdbc, sql, batch, stats, trace, batchNo);
                batch = new ArrayList<>(BATCH_SIZE);
            }
        }
        if (!batch.isEmpty()) {
            batchNo++;
            flushBatch(jdbc, sql, batch, stats, trace, batchNo);
        }
        return stats;
    }

    private void flushBatch(JdbcTemplate jdbc, String sql, List<Object[]> batch, WriteStats stats,
                            DsJobSyncTrace trace, int batchNo) {
        int size = batch.size();
        try {
            int[] results = jdbc.batchUpdate(sql, batch);
            int ok = 0;
            int bad = 0;
            for (int r : results) {
                // SUCCESS_NO_INFO = -2
                if (r >= 0 || r == java.sql.Statement.SUCCESS_NO_INFO) {
                    stats.inserted++;
                    ok++;
                } else {
                    stats.errors++;
                    bad++;
                }
            }
            trace.step("批次#%d 写入完成：size=%d ok=%d bad=%d 累计inserted=%d",
                    batchNo, size, ok, bad, stats.inserted);
        } catch (Exception e) {
            // 批失败时退化为逐行，尽量写入并可统计错误
            log.warn("batch insert failed, fallback to single insert: {}", e.getMessage());
            trace.step("批次#%d 批量失败，改为逐行写入：%s", batchNo, e.getMessage());
            int ok = 0;
            int bad = 0;
            for (Object[] args : batch) {
                try {
                    jdbc.update(sql, args);
                    stats.inserted++;
                    ok++;
                } catch (Exception ex) {
                    stats.errors++;
                    bad++;
                    if (stats.firstError == null) {
                        stats.firstError = ex.getMessage();
                    }
                }
            }
            trace.step("批次#%d 逐行完成：ok=%d bad=%d 累计inserted=%d",
                    batchNo, ok, bad, stats.inserted);
        }
    }

    private Object normalizeJdbcValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Map || value instanceof List || value instanceof com.alibaba.fastjson2.JSONObject
                || value instanceof com.alibaba.fastjson2.JSONArray) {
            return String.valueOf(value);
        }
        return value;
    }

    private boolean tableExists(AbstractDsQueryTool tool, String tableName) {
        List<String> tables = tool.getTableNames();
        if (tables != null) {
            for (String t : tables) {
                if (t != null && t.equalsIgnoreCase(tableName)) {
                    return true;
                }
            }
        }
        return tool.tableExist(tableName);
    }

    private DsJdbcDatasource requireJdbc(Long id) {
        DsJdbcDatasource ds = jdbcDatasourceMapper.selectById(id);
        if (ds == null) {
            throw new IllegalArgumentException("jdbc datasource not found: " + id);
        }
        return ds;
    }

    private void validateIdent(String name, String label) {
        if (StringUtils.isEmpty(name) || !SAFE_IDENT.matcher(name).matches()) {
            throw new IllegalArgumentException("invalid " + label + ": " + name);
        }
    }

    private static final class EnsureSchemaOutcome {
        private boolean tableCreated;
        private int columnsAdded;
        private int columnsModified;
        private boolean primaryKeySynced;
    }

    private static final class WriteStats {
        private int inserted;
        private int errors;
        private String firstError;
    }

    /** Source JDBC column metadata used when creating/adding target columns. */
    private static final class SourceColumnMeta {
        private String name;
        private String columnType;
        private String comment;
        private boolean nullable = true;
        private boolean primaryKey;
        /** information_schema.ORDINAL_POSITION */
        private int ordinalPosition;
    }

    /** 目标表已有列元数据（用于与映射对比后决定是否 MODIFY） */
    private static final class ExistingColumnMeta {
        private String name;
        private String columnType;
        private String comment = "";
        private boolean nullable = true;
    }
}
