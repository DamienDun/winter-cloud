package com.winter.cloud.datasource.service.impl;

import com.winter.cloud.common.core.utils.StringUtils;
import com.winter.cloud.common.datasource.ddl.DsDdlDialect;
import com.winter.cloud.common.datasource.ddl.DsDdlDialectFactory;
import com.winter.cloud.common.datasource.ddl.model.DsColumnDef;
import com.winter.cloud.common.datasource.info.ColumnInfo;
import com.winter.cloud.common.datasource.query.AbstractDsQueryTool;
import com.winter.cloud.datasource.constant.DsJobConstants;
import com.winter.cloud.datasource.domain.dto.DsJobFieldMapping;
import com.winter.cloud.datasource.domain.dto.DsJobSyncResult;
import com.winter.cloud.datasource.domain.entity.DsJdbcDatasource;
import com.winter.cloud.datasource.domain.entity.DsJobInfo;
import com.winter.cloud.datasource.mapper.DsJdbcDatasourceMapper;
import com.winter.cloud.datasource.service.IDsApiDatasourceService;
import com.winter.cloud.datasource.service.IDsJobDataSyncService;
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
import java.util.regex.Pattern;

/**
 * 作业数据同步实现：API/JDBC 读源 → 目标 JDBC 写数；缺表建表、缺列补列
 */
@Service
public class DsJobDataSyncServiceImpl implements IDsJobDataSyncService {

    private static final Logger log = LoggerFactory.getLogger(DsJobDataSyncServiceImpl.class);

    private static final Pattern SAFE_IDENT = Pattern.compile("^[A-Za-z_][A-Za-z0-9_]*$");
    private static final int BATCH_SIZE = 200;

    @Autowired
    private IDsApiDatasourceService apiDatasourceService;

    @Autowired
    private DsJdbcDatasourceMapper jdbcDatasourceMapper;

    @Autowired
    private DsQueryToolSupport queryToolSupport;

    @Override
    public DsJobSyncResult sync(DsJobInfo job) {
        if (job == null) {
            throw new IllegalArgumentException("job required");
        }
        List<DsJobFieldMapping> mappings = requireMappings(job);
        if (job.getTargetDatasourceId() == null || StringUtils.isEmpty(job.getTargetTable())) {
            throw new IllegalArgumentException("target datasource/table required");
        }
        validateIdent(job.getTargetTable(), "targetTable");
        for (DsJobFieldMapping m : mappings) {
            validateIdent(m.getTargetField(), "targetField");
        }

        List<Map<String, Object>> sourceRows = loadSourceRows(job);
        List<Map<String, Object>> targetRows = mapRows(sourceRows, mappings);
        // JDBC source: reuse column type/comment when create/add columns
        Map<String, SourceColumnMeta> sourceColumnMetas = loadSourceColumnMetas(job);

        DsJdbcDatasource targetDs = requireJdbc(job.getTargetDatasourceId());
        DsDdlDialect dialect = DsDdlDialectFactory.get(targetDs.getDatasourceType());

        DsJobSyncResult result = new DsJobSyncResult();
        result.setSourceCount(sourceRows.size());

        AbstractDsQueryTool tool = queryToolSupport.open(job.getTargetDatasourceId());
        try {
            boolean apiSource = isApiSource(job.getDatasourceType());
            EnsureSchemaOutcome schema = ensureTargetSchema(
                    tool, dialect, targetDs, job.getTargetTable(), mappings, targetRows, sourceColumnMetas, apiSource);
            result.setTableCreated(schema.tableCreated);
            result.setColumnsAdded(schema.columnsAdded);

            // 全量：先清空目标表再写入；增量：直接追加
            if (Integer.valueOf(DsJobConstants.SYNC_FULL).equals(job.getSyncType())) {
                clearTargetTable(tool, targetDs.getDatasourceType(), job.getTargetTable());
            }

            WriteStats stats = batchInsert(tool, targetDs.getDatasourceType(), job.getTargetTable(), mappings, targetRows);
            result.setInsertedCount(stats.inserted);
            result.setErrorCount(stats.errors);
            result.setMessage(String.format(
                    Locale.ROOT,
                    "sync done: source=%d inserted=%d errors=%d tableCreated=%s columnsAdded=%d",
                    result.getSourceCount(),
                    result.getInsertedCount(),
                    result.getErrorCount(),
                    result.isTableCreated(),
                    result.getColumnsAdded()));
            if (stats.errors > 0 && stats.inserted == 0) {
                throw new IllegalStateException(result.getMessage() + "; firstError=" + stats.firstError);
            }
            return result;
        } finally {
            queryToolSupport.close(tool);
        }
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
            cleaned.add(copy);
        }
        if (cleaned.isEmpty()) {
            throw new IllegalArgumentException("fieldMappings required");
        }
        return cleaned;
    }

    private List<Map<String, Object>> loadSourceRows(DsJobInfo job) {
        String type = job.getDatasourceType() == null ? "" : job.getDatasourceType().trim().toLowerCase(Locale.ROOT);
        if ("api".equals(type)) {
            return apiDatasourceService.fetchDataRows(job.getDatasourceId());
        }
        if (StringUtils.isEmpty(job.getSourceTable())) {
            throw new IllegalArgumentException("sourceTable required for jdbc source");
        }
        validateIdent(job.getSourceTable(), "sourceTable");
        return loadJdbcSourceRows(job.getDatasourceId(), job.getSourceTable());
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
                                                   boolean apiSource) {
        EnsureSchemaOutcome outcome = new EnsureSchemaOutcome();
        String schema = resolveSchemaName(targetDs, tool);
        boolean exists = tableExists(tool, tableName);
        Map<String, Object> typeSample = buildTypeSample(mappings, targetRows);
        // 映射列表顺序 = 建表/补列顺序（前端可调）；无源序元数据时保持原列表
        List<DsJobFieldMapping> orderedMappings = mappings;
        boolean mysqlLike = isMysqlLike(targetDs.getDatasourceType());
        if (!exists) {
            List<DsColumnDef> columns = new ArrayList<>();
            for (DsJobFieldMapping m : orderedMappings) {
                columns.add(buildColumnDef(m, typeSample.get(m.getTargetField()), sourceColumnMetas, apiSource));
            }
            for (String sql : dialect.buildCreateTableSqls(tableName, "synced by datasource job", columns)) {
                tool.getJdbcTemplate().execute(sql);
            }
            outcome.tableCreated = true;
            outcome.columnsAdded = columns.size();
            log.info("auto create target table {}.{}.{}", schema, tableName, targetDs.getDatasourceName());
            return outcome;
        }

        Set<String> existing = listExistingColumnNames(tool, schema, tableName);
        String lastPresentTarget = null;
        for (DsJobFieldMapping m : orderedMappings) {
            String tf = m.getTargetField();
            if (existing.contains(tf.toLowerCase(Locale.ROOT))) {
                lastPresentTarget = tf;
                continue;
            }
            DsColumnDef col = buildColumnDef(m, typeSample.get(tf), sourceColumnMetas, apiSource);
            // MySQL: ADD ... AFTER 前一列，使补列位置贴近映射顺序
            if (mysqlLike && StringUtils.isNotEmpty(lastPresentTarget)) {
                col.setAfterColumn(lastPresentTarget);
            }
            try {
                for (String sql : dialect.buildAlterColumnSqls(tableName, "ADD", col)) {
                    tool.getJdbcTemplate().execute(sql);
                }
                existing.add(tf.toLowerCase(Locale.ROOT));
                outcome.columnsAdded++;
                lastPresentTarget = tf;
                log.info("auto add column {}.{}.{} type={} comment={} after={}",
                        schema, tableName, tf, col.getType(), col.getComment(), col.getAfterColumn());
            } catch (Exception e) {
                String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase(Locale.ROOT);
                // concurrent / already-exists: treat as present and continue
                if (msg.contains("duplicate column") || msg.contains("already exists")) {
                    existing.add(tf.toLowerCase(Locale.ROOT));
                    lastPresentTarget = tf;
                    log.warn("skip add column {}.{}.{}: {}", schema, tableName, tf, e.getMessage());
                    continue;
                }
                throw e;
            }
        }
        return outcome;
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

    /** Prefer information_schema with explicit schema name. */
    private Set<String> listExistingColumnNames(AbstractDsQueryTool tool, String schema, String tableName) {
        Set<String> existing = new LinkedHashSet<>();
        try {
            List<Map<String, Object>> rows = tool.getJdbcTemplate().queryForList(
                    "SELECT COLUMN_NAME AS name FROM information_schema.COLUMNS "
                            + "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?",
                    schema, tableName);
            if (rows != null) {
                for (Map<String, Object> row : rows) {
                    Object name = row.get("name");
                    if (name != null) {
                        existing.add(String.valueOf(name).toLowerCase(Locale.ROOT));
                    }
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
            if (col != null && StringUtils.isNotEmpty(col.getName())) {
                existing.add(col.getName().toLowerCase(Locale.ROOT));
            }
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
        col.setPrimaryKey(false);
        col.setNullable(true);
        SourceColumnMeta meta = findSourceMeta(mapping.getSourceField(), sourceColumnMetas);
        if (meta != null) {
            col.setNullable(meta.nullable);
            col.setPrimaryKey(meta.primaryKey);
        }

        // 类型：映射配置 > 源列类型 > API默认字符串 / 样例推断
        if (StringUtils.isNotEmpty(mapping.getTargetType())) {
            col.setType(normalizeRawType(mapping.getTargetType()));
        } else if (meta != null && StringUtils.isNotEmpty(meta.columnType)) {
            col.setType(normalizeRawType(meta.columnType));
        } else if (apiSource) {
            col.setType("varchar(255)");
        } else {
            col.setType(inferSqlType(sampleValue));
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
        if ("int".equals(t) || "integer".equals(t)) {
            return "int";
        }
        if ("int(11)".equals(t)) {
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
        if (value instanceof Float || value instanceof Double || value instanceof BigDecimal) {
            return "decimal(20,6)";
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
        int len = s.length();
        if (len <= 64) {
            return "varchar(128)";
        }
        if (len <= 255) {
            return "varchar(512)";
        }
        if (len <= 2000) {
            return "varchar(2000)";
        }
        return "text";
    }

    private void clearTargetTable(AbstractDsQueryTool tool, String dsType, String tableName) {
        String qTable = DsQueryToolSupport.quoteIdent(dsType, tableName);
        tool.getJdbcTemplate().update("DELETE FROM " + qTable);
    }

    private WriteStats batchInsert(AbstractDsQueryTool tool,
                                   String dsType,
                                   String tableName,
                                   List<DsJobFieldMapping> mappings,
                                   List<Map<String, Object>> targetRows) {
        WriteStats stats = new WriteStats();
        if (targetRows.isEmpty()) {
            return stats;
        }
        List<String> cols = new ArrayList<>();
        for (DsJobFieldMapping m : mappings) {
            cols.add(DsQueryToolSupport.quoteIdent(dsType, m.getTargetField()));
        }
        String qTable = DsQueryToolSupport.quoteIdent(dsType, tableName);
        String placeholders = String.join(",", Collections.nCopies(cols.size(), "?"));
        String sql = "INSERT INTO " + qTable + " (" + String.join(",", cols) + ") VALUES (" + placeholders + ")";
        JdbcTemplate jdbc = tool.getJdbcTemplate();

        List<Object[]> batch = new ArrayList<>(BATCH_SIZE);
        for (Map<String, Object> row : targetRows) {
            Object[] args = new Object[mappings.size()];
            for (int i = 0; i < mappings.size(); i++) {
                args[i] = normalizeJdbcValue(row.get(mappings.get(i).getTargetField()));
            }
            batch.add(args);
            if (batch.size() >= BATCH_SIZE) {
                flushBatch(jdbc, sql, batch, stats);
                batch = new ArrayList<>(BATCH_SIZE);
            }
        }
        if (!batch.isEmpty()) {
            flushBatch(jdbc, sql, batch, stats);
        }
        return stats;
    }

    private void flushBatch(JdbcTemplate jdbc, String sql, List<Object[]> batch, WriteStats stats) {
        try {
            int[] results = jdbc.batchUpdate(sql, batch);
            for (int r : results) {
                // SUCCESS_NO_INFO = -2
                if (r >= 0 || r == java.sql.Statement.SUCCESS_NO_INFO) {
                    stats.inserted++;
                } else {
                    stats.errors++;
                }
            }
        } catch (Exception e) {
            // 批失败时退化为逐行，尽量写入并可统计错误
            log.warn("batch insert failed, fallback to single insert: {}", e.getMessage());
            for (Object[] args : batch) {
                try {
                    jdbc.update(sql, args);
                    stats.inserted++;
                } catch (Exception ex) {
                    stats.errors++;
                    if (stats.firstError == null) {
                        stats.firstError = ex.getMessage();
                    }
                }
            }
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
}
