package com.winter.cloud.datasource.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.winter.cloud.common.core.utils.StringUtils;
import com.winter.cloud.datasource.domain.dto.DsApiFieldItem;
import com.winter.cloud.datasource.domain.dto.DsApiReqParams;
import com.winter.cloud.datasource.domain.dto.DsApiReqParamsItem;
import com.winter.cloud.datasource.domain.dto.DsApiTestResult;
import com.winter.cloud.datasource.domain.entity.DsApiDatasource;
import com.winter.cloud.datasource.mapper.DsApiDatasourceMapper;
import com.winter.cloud.datasource.service.IDsApiDatasourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class DsApiDatasourceServiceImpl implements IDsApiDatasourceService {

    private static final int REQ_POST = 0;
    private static final int REQ_GET = 1;
    private static final int AVAILABLE = 0;
    private static final Pattern RELY_PLACEHOLDER = Pattern.compile("\\{\\{rely\\.([0-9a-zA-Z_.]+)\\}\\}");

    @Autowired
    private DsApiDatasourceMapper apiDatasourceMapper;

    @Override
    public List<DsApiDatasource> selectList(DsApiDatasource query) {
        LambdaQueryWrapper<DsApiDatasource> qw = Wrappers.lambdaQuery();
        if (query != null) {
            qw.like(StringUtils.isNotEmpty(query.getDatasourceName()), DsApiDatasource::getDatasourceName, query.getDatasourceName());
            qw.eq(StringUtils.isNotEmpty(query.getDatasourceGroup()), DsApiDatasource::getDatasourceGroup, query.getDatasourceGroup());
            qw.eq(query.getReqType() != null, DsApiDatasource::getReqType, query.getReqType());
            qw.eq(query.getStatus() != null, DsApiDatasource::getStatus, query.getStatus());
            qw.eq(query.getBelongToRelyDatasource() != null, DsApiDatasource::getBelongToRelyDatasource, query.getBelongToRelyDatasource());
        }
        qw.orderByDesc(DsApiDatasource::getGmtModified);
        List<DsApiDatasource> list = apiDatasourceMapper.selectList(qw);
        list.forEach(this::fillReqParams);
        return list;
    }

    @Override
    public DsApiDatasource selectById(Long id) {
        DsApiDatasource entity = apiDatasourceMapper.selectById(id);
        fillReqParams(entity);
        return entity;
    }

    @Override
    public List<DsApiDatasource> selectRelyCandidates(Long excludeId, String datasourceName) {
        // 候选=全部启用接口（排除自身）；勿按 belongToRely 过滤
        LambdaQueryWrapper<DsApiDatasource> qw = Wrappers.<DsApiDatasource>lambdaQuery()
                .eq(DsApiDatasource::getStatus, 1)
                .like(StringUtils.isNotEmpty(datasourceName), DsApiDatasource::getDatasourceName, datasourceName)
                .orderByAsc(DsApiDatasource::getDatasourceName);
        if (excludeId != null) {
            qw.ne(DsApiDatasource::getId, excludeId);
        }
        List<DsApiDatasource> list = apiDatasourceMapper.selectList(qw);
        // 下拉不需要大字段，避免响应过大
        for (DsApiDatasource item : list) {
            item.setReqParamsJson(null);
            item.setReqParams(null);
            item.setRemark(null);
            item.setDatasourceDesc(null);
        }
        return list;
    }

    @Override
    public List<DsApiFieldItem> listOutputFields(Long id) {
        DsApiDatasource ds = selectById(id);
        if (ds == null) {
            throw new IllegalArgumentException("datasource not found");
        }
        DsApiTestResult test = doHttpRequest(ds, null);
        if (test.getTestStatus() == null || test.getTestStatus() != 0) {
            throw new IllegalStateException("rely api test failed: " + test.getTestResult());
        }
        Object parsed = parseByDataItemKey(ds.getDataItemKey(), test.getTestResult());
        Map<String, String> flat = new LinkedHashMap<>();
        flattenFields("", parsed, flat, 0);
        List<DsApiFieldItem> list = new ArrayList<>();
        for (Map.Entry<String, String> e : flat.entrySet()) {
            list.add(new DsApiFieldItem(e.getKey(), e.getValue()));
        }
        return list;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insert(DsApiDatasource entity) {
        normalize(entity);
        checkNameUnique(null, entity.getDatasourceName());
        syncReqParamsJson(entity);
        if (entity.getStatus() == null) {
            entity.setStatus(1);
        }
        if (entity.getBelongToRelyDatasource() == null) {
            entity.setBelongToRelyDatasource(1);
        }
        if (entity.getReqType() == null) {
            entity.setReqType(REQ_GET);
        }
        // no dependency selected -> clear rely id
        if (!Integer.valueOf(0).equals(entity.getBelongToRelyDatasource())) {
            entity.setRelyDatasourceId(null);
        }
        return apiDatasourceMapper.insert(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int update(DsApiDatasource entity) {
        normalize(entity);
        checkNameUnique(entity.getId(), entity.getDatasourceName());
        syncReqParamsJson(entity);
        if (!Integer.valueOf(0).equals(entity.getBelongToRelyDatasource())) {
            entity.setRelyDatasourceId(null);
        }
        return apiDatasourceMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteByIds(Long[] ids) {
        if (ids == null || ids.length == 0) {
            return 0;
        }
        for (Long id : ids) {
            long cnt = apiDatasourceMapper.selectCount(Wrappers.<DsApiDatasource>lambdaQuery()
                    .eq(DsApiDatasource::getRelyDatasourceId, id));
            if (cnt > 0) {
                DsApiDatasource self = apiDatasourceMapper.selectById(id);
                String name = self == null ? String.valueOf(id) : self.getDatasourceName();
                throw new IllegalStateException("datasource is relied by others, cannot delete: " + name);
            }
        }
        return apiDatasourceMapper.deleteBatchIds(Arrays.asList(ids));
    }

    @Override
    public DsApiTestResult testConnection(DsApiDatasource entity) {
        long begin = System.currentTimeMillis();
        try {
            if (entity.getId() != null && StringUtils.isEmpty(entity.getApiUrl())) {
                DsApiDatasource db = selectById(entity.getId());
                if (db == null) {
                    throw new IllegalArgumentException("datasource not found");
                }
                entity = db;
            }
            fillReqParams(entity);
            Map<String, String> relyValues = resolveRelyValues(entity.getRelyDatasourceId());
            DsApiTestResult result = doHttpRequest(entity, relyValues);
            result.setTestTime(System.currentTimeMillis() - begin);
            return result;
        } catch (Exception e) {
            DsApiTestResult result = new DsApiTestResult();
            result.setTestTime(System.currentTimeMillis() - begin);
            result.setTestStatus(1);
            result.setTestResult(e.getMessage() == null ? e.toString() : e.getMessage());
            return result;
        }
    }

    private Map<String, String> resolveRelyValues(Long relyDatasourceId) {
        Map<String, String> map = new LinkedHashMap<>();
        if (relyDatasourceId == null || relyDatasourceId <= 0) {
            return map;
        }
        DsApiDatasource rely = selectById(relyDatasourceId);
        if (rely == null) {
            throw new IllegalStateException("rely datasource not found: " + relyDatasourceId);
        }
        DsApiTestResult relyResult = doHttpRequest(rely, null);
        if (relyResult.getTestStatus() == null || relyResult.getTestStatus() != 0) {
            throw new IllegalStateException("rely api call failed: " + relyResult.getTestResult());
        }
        Object parsed = parseByDataItemKey(rely.getDataItemKey(), relyResult.getTestResult());
        flattenFields("", parsed, map, 0);
        return map;
    }

    private DsApiTestResult doHttpRequest(DsApiDatasource entity, Map<String, String> relyValues) {
        DsApiTestResult result = new DsApiTestResult();
        long begin = System.currentTimeMillis();
        try {
            fillReqParams(entity);
            DsApiReqParams reqParams = copyReqParams(entity.getReqParams());
            if (relyValues != null && !relyValues.isEmpty()) {
                replacePlaceholders(reqParams, relyValues);
            }
            String url = buildUrl(entity.getApiUrl(), reqParams);
            Integer reqType = entity.getReqType() == null ? REQ_GET : entity.getReqType();

            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(30000);
            conn.setInstanceFollowRedirects(true);
            applyHeaders(conn, reqParams.getHeaders());
            conn.setRequestProperty("Accept", "*/*");

            if (REQ_POST == reqType) {
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                String body = reqParams.getBody() == null ? "" : reqParams.getBody();
                if (StringUtils.isEmpty(conn.getRequestProperty("Content-Type"))) {
                    conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
                }
                byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                conn.setFixedLengthStreamingMode(bytes.length);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(bytes);
                }
            } else {
                conn.setRequestMethod("GET");
            }

            int code = conn.getResponseCode();
            InputStream stream = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
            String bodyText = readStream(stream);
            result.setTestTime(System.currentTimeMillis() - begin);
            result.setTestResult(bodyText);
            result.setTestStatus(code >= 200 && code < 300 ? 0 : 1);
            if (result.getTestStatus() == 1 && StringUtils.isEmpty(bodyText)) {
                result.setTestResult("HTTP " + code);
            }
        } catch (Exception e) {
            result.setTestTime(System.currentTimeMillis() - begin);
            result.setTestStatus(1);
            result.setTestResult(e.getMessage() == null ? e.toString() : e.getMessage());
        }
        return result;
    }

    private DsApiReqParams copyReqParams(DsApiReqParams src) {
        if (src == null) {
            return new DsApiReqParams();
        }
        return JSON.parseObject(JSON.toJSONString(src), DsApiReqParams.class);
    }

    private void replacePlaceholders(DsApiReqParams reqParams, Map<String, String> relyValues) {
        if (reqParams == null) {
            return;
        }
        replaceItemList(reqParams.getParams(), relyValues);
        replaceItemList(reqParams.getHeaders(), relyValues);
        replaceItemList(reqParams.getPage(), relyValues);
        reqParams.setBody(replaceText(reqParams.getBody(), relyValues));
    }

    private void replaceItemList(List<DsApiReqParamsItem> items, Map<String, String> relyValues) {
        if (items == null) {
            return;
        }
        for (DsApiReqParamsItem item : items) {
            if (item != null) {
                item.setValue(replaceText(item.getValue(), relyValues));
            }
        }
    }

    private String replaceText(String text, Map<String, String> relyValues) {
        if (StringUtils.isEmpty(text) || relyValues == null || relyValues.isEmpty()) {
            return text;
        }
        Matcher matcher = RELY_PLACEHOLDER.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1);
            String val = relyValues.getOrDefault(key, "");
            matcher.appendReplacement(sb, Matcher.quoteReplacement(val == null ? "" : val));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * support dataItemKey like: data / data.list / jsonObject.data / jsonArray.list
     */
    private Object parseByDataItemKey(String dataItemKey, String raw) {
        if (StringUtils.isEmpty(raw)) {
            return null;
        }
        Object root;
        String trimmed = raw.trim();
        if (trimmed.startsWith("[")) {
            root = JSON.parseArray(trimmed);
        } else {
            root = JSON.parseObject(trimmed);
        }
        if (StringUtils.isEmpty(dataItemKey)) {
            return root;
        }
        String[] parts = dataItemKey.split("\\.");
        int start = 0;
        if (parts.length > 0 && ("jsonObject".equals(parts[0]) || "jsonArray".equals(parts[0]) || "jsonSting".equals(parts[0]))) {
            start = 1;
        }
        Object cur = root;
        for (int i = start; i < parts.length; i++) {
            if (cur == null) {
                return null;
            }
            String key = parts[i];
            Matcher m = Pattern.compile("^(.*)\\[(\\d+)\\]$").matcher(key);
            if (m.find()) {
                String arrKey = m.group(1);
                int idx = Integer.parseInt(m.group(2));
                if (cur instanceof JSONObject) {
                    cur = ((JSONObject) cur).getJSONArray(arrKey);
                }
                if (cur instanceof JSONArray) {
                    cur = ((JSONArray) cur).get(idx);
                } else {
                    return null;
                }
            } else if (cur instanceof JSONObject) {
                cur = ((JSONObject) cur).get(key);
            } else {
                return null;
            }
        }
        return cur;
    }

    private void flattenFields(String prefix, Object node, Map<String, String> out, int depth) {
        if (node == null || depth > 4) {
            return;
        }
        if (node instanceof JSONObject) {
            JSONObject obj = (JSONObject) node;
            for (String key : obj.keySet()) {
                Object val = obj.get(key);
                String path = StringUtils.isEmpty(prefix) ? key : prefix + "." + key;
                if (val instanceof JSONObject || val instanceof JSONArray) {
                    flattenFields(path, val, out, depth + 1);
                } else {
                    out.put(path, val == null ? "" : String.valueOf(val));
                }
            }
        } else if (node instanceof JSONArray) {
            JSONArray arr = (JSONArray) node;
            if (!arr.isEmpty()) {
                // use first element as schema sample
                flattenFields(prefix, arr.get(0), out, depth + 1);
            }
        } else if (StringUtils.isNotEmpty(prefix)) {
            out.put(prefix, String.valueOf(node));
        }
    }

    private void normalize(DsApiDatasource entity) {
        if (entity == null) {
            return;
        }
        if (StringUtils.isNotEmpty(entity.getDatasourceName())) {
            entity.setDatasourceName(entity.getDatasourceName().trim());
        }
        if (StringUtils.isNotEmpty(entity.getApiUrl())) {
            entity.setApiUrl(entity.getApiUrl().trim());
        }
        if (StringUtils.isNotEmpty(entity.getDatasourceGroup())) {
            entity.setDatasourceGroup(entity.getDatasourceGroup().trim());
        } else {
            entity.setDatasourceGroup("Default");
        }
    }

    private void checkNameUnique(Long id, String name) {
        if (StringUtils.isEmpty(name)) {
            throw new IllegalArgumentException("datasourceName required");
        }
        LambdaQueryWrapper<DsApiDatasource> qw = Wrappers.<DsApiDatasource>lambdaQuery()
                .eq(DsApiDatasource::getDatasourceName, name);
        if (id != null) {
            qw.ne(DsApiDatasource::getId, id);
        }
        if (apiDatasourceMapper.selectCount(qw) > 0) {
            throw new IllegalArgumentException("datasource name already exists");
        }
    }

    private void syncReqParamsJson(DsApiDatasource entity) {
        if (entity.getReqParams() != null) {
            entity.setReqParamsJson(JSON.toJSONString(entity.getReqParams()));
        } else if (entity.getReqParamsJson() == null) {
            entity.setReqParamsJson("{}");
        }
    }

    private void fillReqParams(DsApiDatasource entity) {
        if (entity == null) {
            return;
        }
        if (StringUtils.isNotEmpty(entity.getReqParamsJson())) {
            entity.setReqParams(JSON.parseObject(entity.getReqParamsJson(), DsApiReqParams.class));
        }
        if (entity.getReqParams() == null) {
            entity.setReqParams(new DsApiReqParams());
        }
    }

    private String buildUrl(String apiUrl, DsApiReqParams reqParams) throws Exception {
        String base = apiUrl == null ? "" : apiUrl.trim();
        List<String> pairs = new ArrayList<>();
        if (reqParams != null) {
            appendQueryPairs(pairs, reqParams.getParams());
            appendQueryPairs(pairs, reqParams.getPage());
        }
        if (pairs.isEmpty()) {
            return base;
        }
        String query = String.join("&", pairs);
        return base.contains("?") ? (base + "&" + query) : (base + "?" + query);
    }

    private void appendQueryPairs(List<String> pairs, List<DsApiReqParamsItem> items) throws Exception {
        if (items == null) {
            return;
        }
        for (DsApiReqParamsItem item : items) {
            if (item == null || StringUtils.isEmpty(item.getName())) {
                continue;
            }
            if (item.getIsAvailable() != null && item.getIsAvailable() != AVAILABLE) {
                continue;
            }
            String v = item.getValue() == null ? "" : item.getValue();
            pairs.add(URLEncoder.encode(item.getName(), "UTF-8") + "=" + URLEncoder.encode(v, "UTF-8"));
        }
    }

    private void applyHeaders(HttpURLConnection conn, List<DsApiReqParamsItem> headers) {
        if (headers == null) {
            return;
        }
        for (DsApiReqParamsItem item : headers) {
            if (item == null || StringUtils.isEmpty(item.getName())) {
                continue;
            }
            if (item.getIsAvailable() != null && item.getIsAvailable() != AVAILABLE) {
                continue;
            }
            conn.setRequestProperty(item.getName(), item.getValue() == null ? "" : item.getValue());
        }
    }

    private String readStream(InputStream stream) throws Exception {
        if (stream == null) {
            return "";
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }
}
