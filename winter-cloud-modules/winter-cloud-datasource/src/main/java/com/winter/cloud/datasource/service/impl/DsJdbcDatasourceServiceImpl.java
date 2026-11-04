package com.winter.cloud.datasource.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.winter.cloud.common.core.utils.StringUtils;
import com.winter.cloud.common.datasource.info.DatasourceInfo;
import com.winter.cloud.common.datasource.query.AbstractDsQueryTool;
import com.winter.cloud.common.datasource.query.DsQueryTool;
import com.winter.cloud.common.datasource.query.DsQueryToolFactory;
import com.winter.cloud.common.datasource.util.AESUtil;
import com.winter.cloud.datasource.domain.entity.DsJdbcDatasource;
import com.winter.cloud.datasource.mapper.DsJdbcDatasourceMapper;
import com.winter.cloud.datasource.service.IDsJdbcDatasourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

/**
 * 数据源管理（对齐 datax-web：CRUD + 连接测试）
 */
@Service
public class DsJdbcDatasourceServiceImpl implements IDsJdbcDatasourceService {

    @Autowired
    private DsJdbcDatasourceMapper datasourceMapper;

    @Override
    public List<DsJdbcDatasource> selectList(DsJdbcDatasource query) {
        LambdaQueryWrapper<DsJdbcDatasource> qw = Wrappers.lambdaQuery();
        if (query != null) {
            qw.like(StringUtils.isNotEmpty(query.getDatasourceName()), DsJdbcDatasource::getDatasourceName, query.getDatasourceName());
            qw.eq(StringUtils.isNotEmpty(query.getDatasourceType()), DsJdbcDatasource::getDatasourceType, query.getDatasourceType());
            qw.eq(StringUtils.isNotEmpty(query.getDatasourceGroup()), DsJdbcDatasource::getDatasourceGroup, query.getDatasourceGroup());
            qw.eq(query.getStatus() != null, DsJdbcDatasource::getStatus, query.getStatus());
        }
        qw.orderByDesc(DsJdbcDatasource::getGmtModified);
        return datasourceMapper.selectList(qw);
    }

    @Override
    public DsJdbcDatasource selectById(Long id) {
        DsJdbcDatasource entity = datasourceMapper.selectById(id);
        if (entity != null) {
            // 前端编辑不回显密文密码
            entity.setJdbcPassword(null);
        }
        return entity;
    }

    @Override
    public List<DsJdbcDatasource> selectAllEnabled() {
        return datasourceMapper.selectList(Wrappers.<DsJdbcDatasource>lambdaQuery()
                .eq(DsJdbcDatasource::getStatus, 1)
                .orderByAsc(DsJdbcDatasource::getDatasourceName));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insert(DsJdbcDatasource entity) {
        normalize(entity);
        encryptPasswordIfNeed(entity, true);
        if (entity.getStatus() == null) {
            entity.setStatus(1);
        }
        if (StringUtils.isEmpty(entity.getDatasourceGroup())) {
            entity.setDatasourceGroup("Default");
        }
        return datasourceMapper.insert(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int update(DsJdbcDatasource entity) {
        normalize(entity);
        // 未传密码则保留库中密文
        if (StringUtils.isEmpty(entity.getJdbcPassword())) {
            entity.setJdbcPassword(null);
        } else {
            encryptPasswordIfNeed(entity, false);
        }
        return datasourceMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteByIds(Long[] ids) {
        if (ids == null || ids.length == 0) {
            return 0;
        }
        return datasourceMapper.deleteBatchIds(Arrays.asList(ids));
    }

    @Override
    public boolean testConnection(DsJdbcDatasource entity) {
        if (entity == null) {
            return false;
        }
        // 已保存记录且未带密码：用库中密码
        if (entity.getId() != null && StringUtils.isEmpty(entity.getJdbcPassword())) {
            DsJdbcDatasource db = datasourceMapper.selectById(entity.getId());
            if (db == null) {
                throw new IllegalArgumentException("数据源不存在");
            }
            entity.setJdbcUsername(StringUtils.isEmpty(entity.getJdbcUsername()) ? db.getJdbcUsername() : entity.getJdbcUsername());
            entity.setJdbcPassword(db.getJdbcPassword());
            entity.setJdbcUrl(StringUtils.isEmpty(entity.getJdbcUrl()) ? db.getJdbcUrl() : entity.getJdbcUrl());
            entity.setJdbcDriverClass(StringUtils.isEmpty(entity.getJdbcDriverClass()) ? db.getJdbcDriverClass() : entity.getJdbcDriverClass());
            entity.setDatasourceType(StringUtils.isEmpty(entity.getDatasourceType()) ? db.getDatasourceType() : entity.getDatasourceType());
            entity.setDatabaseName(StringUtils.isEmpty(entity.getDatabaseName()) ? db.getDatabaseName() : entity.getDatabaseName());
        }

        DatasourceInfo info = toDatasourceInfo(entity);
        // 表单明文 or 库中密文：先尝试解密，解不开则当明文
        boolean looksEncrypted = AESUtil.decrypt(info.getJdbcPassword()) != null;
        info.setAesEncryptPassword(looksEncrypted);
        info.setDisableCache(true);

        AbstractDsQueryTool tool = null;
        try {
            DsQueryTool queryTool = DsQueryToolFactory.getByDbType(info);
            tool = (AbstractDsQueryTool) queryTool;
            Integer one = tool.getJdbcTemplate().queryForObject(info.getValidationQuery(), Integer.class);
            return one != null;
        } finally {
            if (tool != null) {
                tool.close();
            }
        }
    }

    private void normalize(DsJdbcDatasource entity) {
        if (entity == null) {
            return;
        }
        if (StringUtils.isNotEmpty(entity.getDatasourceType())) {
            entity.setDatasourceType(entity.getDatasourceType().trim().toLowerCase());
        }
    }

    private void encryptPasswordIfNeed(DsJdbcDatasource entity, boolean required) {
        String pwd = entity.getJdbcPassword();
        if (StringUtils.isEmpty(pwd)) {
            if (required) {
                throw new IllegalArgumentException("密码不能为空");
            }
            return;
        }
        // 已是密文则不重复加密
        if (AESUtil.decrypt(pwd) != null) {
            return;
        }
        String enc = AESUtil.encrypt(pwd);
        if (StringUtils.isEmpty(enc)) {
            throw new IllegalStateException("密码加密失败");
        }
        entity.setJdbcPassword(enc);
    }

    private DatasourceInfo toDatasourceInfo(DsJdbcDatasource entity) {
        DatasourceInfo info = new DatasourceInfo();
        info.setKey("test:" + (entity.getId() != null ? entity.getId() : System.nanoTime()));
        info.setDatasourceName(entity.getDatasourceName());
        info.setDatasourceType(entity.getDatasourceType());
        info.setDatabase(entity.getDatabaseName());
        info.setJdbcUsername(entity.getJdbcUsername());
        info.setJdbcPassword(entity.getJdbcPassword());
        info.setJdbcUrl(entity.getJdbcUrl());
        info.setJdbcDriverClass(entity.getJdbcDriverClass());
        return info;
    }
}
