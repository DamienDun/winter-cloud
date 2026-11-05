package com.winter.cloud.datasource.service.impl;

import com.winter.cloud.common.core.utils.StringUtils;
import com.winter.cloud.common.datasource.ddl.DsDdlDialect;
import com.winter.cloud.common.datasource.ddl.DsDdlDialectFactory;
import com.winter.cloud.common.datasource.ddl.model.DsColumnDef;
import com.winter.cloud.common.datasource.ddl.model.DsIndexRequest;
import com.winter.cloud.common.datasource.query.AbstractDsQueryTool;
import com.winter.cloud.datasource.domain.dto.DsAlterColumnRequest;
import com.winter.cloud.datasource.domain.dto.DsCreateTableRequest;
import com.winter.cloud.datasource.domain.dto.DsDropTableRequest;
import com.winter.cloud.datasource.domain.entity.DsJdbcDatasource;
import com.winter.cloud.datasource.mapper.DsJdbcDatasourceMapper;
import com.winter.cloud.datasource.service.IDsTableSchemaService;
import com.winter.cloud.datasource.support.DsQueryToolSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 表结构 DDL：按方言路由（MySQL / PostgreSQL / Oracle / SQL Server 等）
 */
@Service
public class DsTableSchemaServiceImpl implements IDsTableSchemaService {

    @Autowired
    private DsQueryToolSupport queryToolSupport;

    @Autowired
    private DsJdbcDatasourceMapper datasourceMapper;

    @Override
    public void createTable(DsCreateTableRequest request) {
        DsJdbcDatasource ds = requireDs(request.getDatasourceId());
        DsDdlDialect dialect = DsDdlDialectFactory.get(ds.getDatasourceType());
        AbstractDsQueryTool tool = queryToolSupport.open(request.getDatasourceId());
        try {
            String table = request.getTableName().trim();
            DsQueryToolSupport.quoteIdent(ds.getDatasourceType(), table);
            List<String> names = tool.getTableNames();
            boolean exists = names.stream().anyMatch(table::equalsIgnoreCase);
            if (exists) {
                throw new IllegalArgumentException("表已存在：" + table);
            }
            if (request.getColumns() == null || request.getColumns().isEmpty()) {
                throw new IllegalArgumentException("至少需要一列");
            }
            for (String sql : dialect.buildCreateTableSqls(table, request.getComment(), request.getColumns())) {
                tool.getJdbcTemplate().execute(sql);
            }
        } finally {
            queryToolSupport.close(tool);
        }
    }

    @Override
    public void dropTable(DsDropTableRequest request) {
        if (!request.getTableName().equals(request.getConfirmTableName())) {
            throw new IllegalArgumentException("二次确认表名不一致，已取消删除");
        }
        DsJdbcDatasource ds = requireDs(request.getDatasourceId());
        DsDdlDialect dialect = DsDdlDialectFactory.get(ds.getDatasourceType());
        AbstractDsQueryTool tool = queryToolSupport.open(request.getDatasourceId());
        try {
            assertTableExists(tool, ds.getDatasourceType(), request.getTableName());
            tool.getJdbcTemplate().execute(dialect.buildDropTableSql(request.getTableName()));
        } finally {
            queryToolSupport.close(tool);
        }
    }

    @Override
    public void alterColumn(DsAlterColumnRequest request) {
        DsJdbcDatasource ds = requireDs(request.getDatasourceId());
        DsDdlDialect dialect = DsDdlDialectFactory.get(ds.getDatasourceType());
        String action = request.getAction() == null ? "" : request.getAction().trim().toUpperCase(Locale.ROOT);
        AbstractDsQueryTool tool = queryToolSupport.open(request.getDatasourceId());
        try {
            assertTableExists(tool, ds.getDatasourceType(), request.getTableName());
            DsColumnDef col = request.getColumn();
            if (col == null || StringUtils.isEmpty(col.getName())) {
                throw new IllegalArgumentException("列名不能为空");
            }
            for (String sql : dialect.buildAlterColumnSqls(request.getTableName(), action, col)) {
                tool.getJdbcTemplate().execute(sql);
            }
        } finally {
            queryToolSupport.close(tool);
        }
    }

    @Override
    public void createIndex(DsIndexRequest request) {
        DsJdbcDatasource ds = requireDs(request.getDatasourceId());
        DsDdlDialect dialect = DsDdlDialectFactory.get(ds.getDatasourceType());
        AbstractDsQueryTool tool = queryToolSupport.open(request.getDatasourceId());
        try {
            assertTableExists(tool, ds.getDatasourceType(), request.getTableName());
            tool.getJdbcTemplate().execute(dialect.buildCreateIndexSql(request));
        } finally {
            queryToolSupport.close(tool);
        }
    }

    @Override
    public void alterIndex(DsIndexRequest request) {
        if (StringUtils.isEmpty(request.getOldIndexName())) {
            throw new IllegalArgumentException("修改索引时原索引名不能为空");
        }
        if ("PRIMARY".equalsIgnoreCase(request.getOldIndexName())) {
            throw new IllegalArgumentException("不允许修改主键索引 PRIMARY");
        }
        DsJdbcDatasource ds = requireDs(request.getDatasourceId());
        DsDdlDialect dialect = DsDdlDialectFactory.get(ds.getDatasourceType());
        AbstractDsQueryTool tool = queryToolSupport.open(request.getDatasourceId());
        try {
            assertTableExists(tool, ds.getDatasourceType(), request.getTableName());
            tool.getJdbcTemplate().execute(dialect.buildDropIndexSql(request.getTableName(), request.getOldIndexName()));
            tool.getJdbcTemplate().execute(dialect.buildCreateIndexSql(request));
        } finally {
            queryToolSupport.close(tool);
        }
    }

    @Override
    public void dropIndex(DsIndexRequest request) {
        DsJdbcDatasource ds = requireDs(request.getDatasourceId());
        DsDdlDialect dialect = DsDdlDialectFactory.get(ds.getDatasourceType());
        AbstractDsQueryTool tool = queryToolSupport.open(request.getDatasourceId());
        try {
            assertTableExists(tool, ds.getDatasourceType(), request.getTableName());
            tool.getJdbcTemplate().execute(dialect.buildDropIndexSql(request.getTableName(), request.getIndexName()));
        } finally {
            queryToolSupport.close(tool);
        }
    }

    @Override
    public List<Map<String, Object>> listIndexes(Long datasourceId, String tableName) {
        DsJdbcDatasource ds = requireDs(datasourceId);
        DsDdlDialect dialect = DsDdlDialectFactory.get(ds.getDatasourceType());
        AbstractDsQueryTool tool = queryToolSupport.open(datasourceId);
        try {
            assertTableExists(tool, ds.getDatasourceType(), tableName);
            return dialect.listIndexes(tool.getJdbcTemplate(), tableName);
        } finally {
            queryToolSupport.close(tool);
        }
    }

    @Override
    public Map<String, Object> capabilities(Long datasourceId) {
        DsJdbcDatasource ds = requireDs(datasourceId);
        DsDdlDialect dialect = DsDdlDialectFactory.get(ds.getDatasourceType());
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("ddlSupported", true);
        map.put("dialect", dialect.name());
        map.put("datasourceType", ds.getDatasourceType());
        map.put("indexTypes", dialect.supportedIndexTypes());
        map.put("indexMethods", dialect.supportedIndexMethods());
        map.put("supportsIndexComment", dialect.supportsIndexComment());
        return map;
    }

    private DsJdbcDatasource requireDs(Long id) {
        DsJdbcDatasource ds = datasourceMapper.selectById(id);
        if (ds == null) {
            throw new IllegalArgumentException("数据源不存在");
        }
        // 提前校验方言，给出统一错误信息
        DsDdlDialectFactory.get(ds.getDatasourceType());
        return ds;
    }

    private void assertTableExists(AbstractDsQueryTool tool, String dsType, String tableName) {
        if (StringUtils.isEmpty(tableName)) {
            throw new IllegalArgumentException("表名不能为空");
        }
        DsQueryToolSupport.quoteIdent(dsType, tableName);
        List<String> tables = tool.getTableNames();
        boolean ok = tables.stream().anyMatch(t -> tableName.equalsIgnoreCase(t));
        if (!ok && !tool.tableExist(tableName)) {
            throw new IllegalArgumentException("表不存在：" + tableName);
        }
    }
}
