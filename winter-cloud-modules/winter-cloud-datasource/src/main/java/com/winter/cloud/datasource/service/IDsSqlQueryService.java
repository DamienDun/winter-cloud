package com.winter.cloud.datasource.service;

import com.winter.cloud.datasource.domain.dto.DsSqlQueryRequest;

import java.util.Map;

/**
 * Readonly custom SQL query
 */
public interface IDsSqlQueryService {

    Map<String, Object> query(DsSqlQueryRequest request);
}