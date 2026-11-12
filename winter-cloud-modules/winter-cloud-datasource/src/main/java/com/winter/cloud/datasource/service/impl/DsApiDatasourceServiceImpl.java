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
import com.winter.cloud.datasource.support.DsJobCancelRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class DsApiDatasourceServiceImpl implements IDsApiDatasourceService {

    private static final Logger log = LoggerFactory.getLogger(DsApiDatasourceServiceImpl.class);

    private static final int REQ_POST = 0;
    private static final int REQ_GET = 1;
    private static final int AVAILABLE = 0;
    private static final Pattern RELY_PLACEHOLDER = Pattern.compile("\\{\\{rely\\.([0-9a-zA-Z_.]+)\\}\\}");

    @Autowired
    private DsApiDatasourceMapper apiDatasourceMapper;

    @Autowired
    private DsJobCancelRegistry jobCancelRegistry;

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
        // 与同步取数一致：先解析依赖 token，再调业务接口
        Map<String, String> relyValues = resolveRelyValues(ds.getRelyDatasourceId());
        DsApiTestResult test = doHttpRequest(ds, relyValues, null, isPageApi(ds));
        if (test.getTestStatus() == null || test.getTestStatus() != 0) {
            throw new IllegalStateException("api call failed: " + test.getTestResult());
        }
        Object root = parseRootJson(test.getTestResult());
        // 始终从根按 JSON 路径展平（数组只取第一条），得到 data.id 这种唯一路径；值保留原始类型用于推断
        Map<String, Object> flat = new LinkedHashMap<>();
        flattenJsonKeys("", root, flat, 0);

        String scopePrefix = normalizeDataItemKeyPrefix(ds.getDataItemKey());
        if (StringUtils.isEmpty(scopePrefix)) {
            // 未配 dataItemKey 时，自动落到业务列表路径（如 data / data.list）
            Object parsed = parseByDataItemKey(null, test.getTestResult());
            RowExtract extract = extractRowItems(root, parsed, null);
            if (extract != null && StringUtils.isNotEmpty(extract.prefix)) {
                scopePrefix = extract.prefix;
            }
        }
        if (StringUtils.isNotEmpty(scopePrefix)) {
            final String prefix = scopePrefix;
            flat.entrySet().removeIf(e -> {
                String k = e.getKey();
                return k == null || !(k.equals(prefix) || k.startsWith(prefix + "."));
            });
        }
        // 去掉被完整路径覆盖的末段重复项（保留 data.id，去掉多余的 id）
        removeShadowedLeafKeys(flat);

        List<DsApiFieldItem> list = new ArrayList<>();
        for (Map.Entry<String, Object> e : flat.entrySet()) {
            Object sample = e.getValue();
            String sampleText = sample == null ? "" : String.valueOf(sample);
            list.add(new DsApiFieldItem(e.getKey(), sampleText, inferApiSqlType(sample)));
        }
        log.info("listOutputFields id={} fieldCount={} scopePrefix={}", id, list.size(), scopePrefix);
        return list;
    }

    /**
     * 若同时存在 id 与 data.id，只保留带路径的 data.id，避免作业映射出现多余末段字段。
     */
    private void removeShadowedLeafKeys(Map<String, ?> flat) {
        if (flat == null || flat.isEmpty()) {
            return;
        }
        List<String> keys = new ArrayList<>(flat.keySet());
        for (String key : keys) {
            if (key == null || key.contains(".")) {
                continue;
            }
            boolean shadowed = false;
            for (String other : keys) {
                if (other != null && other.endsWith("." + key)) {
                    shadowed = true;
                    break;
                }
            }
            if (shadowed) {
                flat.remove(key);
            }
        }
    }

    /**
     * 按 JSON 结构展平字段 key（点路径，不重复）。
     * 遇到数组只取第一条元素继续解析，不展开全部下标；叶子值保留原始类型。
     */
    private void flattenJsonKeys(String prefix, Object node, Map<String, Object> out, int depth) {
        if (node == null || depth > 8) {
            return;
        }
        if (node instanceof JSONArray) {
            JSONArray arr = (JSONArray) node;
            if (arr.isEmpty()) {
                return;
            }
            // 数组类型：只拿第一条解析里面的 key
            flattenJsonKeys(prefix, arr.get(0), out, depth + 1);
            return;
        }
        if (node instanceof JSONObject) {
            JSONObject obj = (JSONObject) node;
            for (String key : obj.keySet()) {
                if (StringUtils.isEmpty(key)) {
                    continue;
                }
                Object val = obj.get(key);
                String path = StringUtils.isEmpty(prefix) ? key : prefix + "." + key;
                if (val instanceof JSONObject || val instanceof JSONArray) {
                    flattenJsonKeys(path, val, out, depth + 1);
                } else {
                    out.putIfAbsent(path, val);
                }
            }
            return;
        }
        if (node instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) node;
            for (Map.Entry<String, Object> e : map.entrySet()) {
                if (StringUtils.isEmpty(e.getKey())) {
                    continue;
                }
                Object val = e.getValue();
                String path = StringUtils.isEmpty(prefix) ? e.getKey() : prefix + "." + e.getKey();
                if (val instanceof JSONObject || val instanceof JSONArray || val instanceof Map || val instanceof List) {
                    flattenJsonKeys(path, val, out, depth + 1);
                } else {
                    out.putIfAbsent(path, val);
                }
            }
            return;
        }
        if (node instanceof List) {
            List<?> list = (List<?>) node;
            if (!list.isEmpty()) {
                flattenJsonKeys(prefix, list.get(0), out, depth + 1);
            }
            return;
        }
        if (StringUtils.isNotEmpty(prefix)) {
            out.putIfAbsent(prefix, node);
        }
    }

    /** 根据接口样例值推断目标列 SQL 类型 */
    private String inferApiSqlType(Object value) {
        if (value == null) {
            return "varchar(255)";
        }
        if (value instanceof Boolean) {
            return "tinyint(1)";
        }
        if (value instanceof Byte || value instanceof Short || value instanceof Integer) {
            return "int";
        }
        if (value instanceof Long) {
            return "bigint";
        }
        // 小数一律 varchar，避免 decimal/浮点入库丢精度
        if (value instanceof Float || value instanceof Double || value instanceof java.math.BigDecimal) {
            return varcharByLength(String.valueOf(value));
        }
        if (value instanceof java.util.Date || value instanceof java.sql.Timestamp || value instanceof java.sql.Date
                || value instanceof java.time.LocalDateTime || value instanceof java.time.LocalDate) {
            return "datetime";
        }
        if (value instanceof Number) {
            // Fastjson 等可能给出其它 Number 实现
            String n = String.valueOf(value);
            if (n.contains(".") || n.contains("e") || n.contains("E")) {
                return varcharByLength(n);
            }
            try {
                long lv = ((Number) value).longValue();
                if (lv > Integer.MAX_VALUE || lv < Integer.MIN_VALUE) {
                    return "bigint";
                }
                return "int";
            } catch (Exception ignore) {
                return "bigint";
            }
        }
        String s = String.valueOf(value).trim();
        if (s.isEmpty()) {
            return "varchar(255)";
        }
        if ("true".equalsIgnoreCase(s) || "false".equalsIgnoreCase(s)) {
            return "tinyint(1)";
        }
        if (s.matches("^\\d{4}-\\d{2}-\\d{2}([ T]\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?)?$")) {
            return "datetime";
        }
        if (s.matches("^-?\\d+$")) {
            if (s.length() > 10) {
                return "bigint";
            }
            return "int";
        }
        // 小数文本也走 varchar，避免精度丢失
        if (s.matches("^-?\\d+(\\.\\d+)?([eE][+-]?\\d+)?$") && s.indexOf('.') >= 0) {
            return varcharByLength(s);
        }
        return varcharByLength(s);
    }

    private String varcharByLength(String s) {
        int len = s == null ? 0 : s.trim().length();
        if (len <= 64) {
            return "varchar(128)";
        }
        if (len <= 255) {
            return "varchar(255)";
        }
        if (len <= 512) {
            return "varchar(512)";
        }
        if (len <= 2000) {
            return "varchar(2000)";
        }
        return "text";
    }

    /** 自动翻页上限，防止分页参数无效时死循环 */
    private static final int MAX_API_PAGES = 500;
    /** 测试连接时分页接口强制的 pageSize，避免一次返回过多导致页面卡顿 */
    private static final int TEST_PAGE_SIZE = 2;
    private static final String[] PAGE_NUM_KEYS = {"pagenum", "pageno", "page", "current", "currentpage"};
    private static final String[] PAGE_SIZE_KEYS = {"pagesize", "limit", "size"};

    @Override
    public List<Map<String, Object>> fetchDataRows(Long id) {
        // 兼容旧调用：仍可能全量累加；作业同步请走 forEachDataPage
        List<Map<String, Object>> all = new ArrayList<>();
        forEachDataPage(id, (pageNum, rows) -> {
            if (rows != null && !rows.isEmpty()) {
                all.addAll(rows);
            }
        });
        return all;
    }

    @Override
    public int forEachDataPage(Long id, ApiPageConsumer pageConsumer) {
        if (pageConsumer == null) {
            throw new IllegalArgumentException("pageConsumer required");
        }
        DsApiDatasource ds = selectById(id);
        if (ds == null) {
            throw new IllegalArgumentException("datasource not found");
        }
        fillReqParams(ds);
        Map<String, String> relyValues = resolveRelyValues(ds.getRelyDatasourceId());
        boolean pageApi = isPageApi(ds);
        String pageNumKey = detectPageNumKey(ds.getReqParams());
        log.info("api forEachDataPage start id={} name={} pageApi={} pageNumKey={}",
                id, ds.getDatasourceName(), pageApi, pageNumKey);

        // 非分页：只请求一次
        if (!pageApi) {
            jobCancelRegistry.throwIfCancelled();
            DsApiTestResult test = doHttpRequest(ds, relyValues, null, false);
            jobCancelRegistry.throwIfCancelled();
            List<Map<String, Object>> rows = parseResponseToRows(ds, test);
            try {
                pageConsumer.accept(1, rows);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
            log.info("api forEachDataPage done(single) id={} rows={}", id, rows.size());
            return rows.size();
        }

        // 分页：逐页回调，不在此累加
        int total = 0;
        int page = 1;
        for (; page <= MAX_API_PAGES; page++) {
            jobCancelRegistry.throwIfCancelled();
            DsApiTestResult test = doHttpRequest(ds, relyValues, page, false);
            jobCancelRegistry.throwIfCancelled();
            if (test.getTestStatus() == null || test.getTestStatus() != 0) {
                throw new IllegalStateException("api call failed at page=" + page + ": " + test.getTestResult());
            }
            List<Map<String, Object>> pageRows = parseResponseToRows(ds, test);
            log.info("api forEachDataPage page={} rows={}", page, pageRows.size());
            if (pageRows.isEmpty()) {
                try {
                    pageConsumer.accept(page, pageRows);
                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    throw new IllegalStateException(e.getMessage(), e);
                }
                break;
            }
            try {
                pageConsumer.accept(page, pageRows);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
            total += pageRows.size();
        }
        if (page > MAX_API_PAGES) {
            log.warn("api forEachDataPage hit max pages={}, id={}, rows={}", MAX_API_PAGES, id, total);
        }
        log.info("api forEachDataPage done(paged) id={} pagesTried={} totalRows={}",
                id, Math.min(page, MAX_API_PAGES), total);
        return total;
    }

    private boolean isPageApi(DsApiDatasource ds) {
        return ds != null && Integer.valueOf(1).equals(ds.getPageApi());
    }

    /**
     * 将单次 API 响应解析为扁平行；HTTP 失败直接抛错。
     */
    private List<Map<String, Object>> parseResponseToRows(DsApiDatasource ds, DsApiTestResult test) {
        if (test.getTestStatus() == null || test.getTestStatus() != 0) {
            throw new IllegalStateException("api call failed: " + test.getTestResult());
        }
        String raw = test.getTestResult();
        Object root = parseRootJson(raw);
        String dataItemKey = ds.getDataItemKey();
        Object parsed = parseByDataItemKey(dataItemKey, raw);
        RowExtract extract = extractRowItems(root, parsed, dataItemKey);
        List<Map<String, Object>> rows = new ArrayList<>();
        if (extract == null || extract.items == null || extract.items.isEmpty()) {
            return rows;
        }
        for (Object item : extract.items) {
            if (!(item instanceof JSONObject) && !(item instanceof Map)) {
                continue;
            }
            Map<String, Object> flat = new LinkedHashMap<>();
            flattenValueFields("", item, flat, 0);
            if (StringUtils.isNotEmpty(extract.prefix)) {
                Map<String, Object> prefixed = new LinkedHashMap<>();
                for (Map.Entry<String, Object> e : flat.entrySet()) {
                    prefixed.put(extract.prefix + "." + e.getKey(), e.getValue());
                }
                flat.putAll(prefixed);
            }
            if (root instanceof JSONObject) {
                JSONObject rootObj = (JSONObject) root;
                for (String key : rootObj.keySet()) {
                    Object val = rootObj.get(key);
                    if (!(val instanceof JSONObject) && !(val instanceof JSONArray)
                            && !(val instanceof Map) && !(val instanceof List)) {
                        flat.putIfAbsent(key, val);
                    }
                }
            }
            rows.add(flat);
        }
        return rows;
    }

    /** 从入参/分页参数/Body 中识别页码字段名（如 pageNum） */
    private String detectPageNumKey(DsApiReqParams reqParams) {
        if (reqParams == null) {
            return null;
        }
        String fromPage = findPageNumKeyInItems(reqParams.getPage());
        if (fromPage != null) {
            return fromPage;
        }
        String fromParams = findPageNumKeyInItems(reqParams.getParams());
        if (fromParams != null) {
            return fromParams;
        }
        return findPageNumKeyInBody(reqParams.getBody());
    }

    private String findPageNumKeyInItems(List<DsApiReqParamsItem> items) {
        if (items == null) {
            return null;
        }
        for (DsApiReqParamsItem item : items) {
            if (item == null || StringUtils.isEmpty(item.getName())) {
                continue;
            }
            if (item.getIsAvailable() != null && item.getIsAvailable() != AVAILABLE) {
                continue;
            }
            String name = item.getName().trim();
            if (isPageNumKey(name)) {
                return name;
            }
        }
        return null;
    }

    private String findPageNumKeyInBody(String body) {
        if (StringUtils.isEmpty(body)) {
            return null;
        }
        String trimmed = body.trim();
        if (!trimmed.startsWith("{")) {
            return null;
        }
        try {
            JSONObject obj = JSON.parseObject(trimmed);
            if (obj == null) {
                return null;
            }
            for (String key : obj.keySet()) {
                if (isPageNumKey(key)) {
                    return key;
                }
            }
        } catch (Exception ignore) {
            // body 非 JSON 时忽略
        }
        return null;
    }

    private boolean isPageNumKey(String name) {
        if (StringUtils.isEmpty(name)) {
            return false;
        }
        String n = name.trim().toLowerCase(Locale.ROOT).replace("_", "");
        for (String k : PAGE_NUM_KEYS) {
            if (k.equals(n)) {
                return true;
            }
        }
        return false;
    }

    /** 把页码写入 page/params/body 中已存在的页码字段 */
    private void applyPageNumber(DsApiReqParams reqParams, String pageNumKey, int pageNum) {
        if (reqParams == null || StringUtils.isEmpty(pageNumKey)) {
            return;
        }
        String val = String.valueOf(pageNum);
        setItemValueByName(reqParams.getPage(), pageNumKey, val);
        setItemValueByName(reqParams.getParams(), pageNumKey, val);
        reqParams.setBody(setJsonNumberOrStringField(reqParams.getBody(), pageNumKey, pageNum));
    }

    /** 测试连接：把 pageSize/limit/size 等改为较小值（若参数或 Body 中存在该字段） */
    private void applyPageSize(DsApiReqParams reqParams, int pageSize) {
        if (reqParams == null) {
            return;
        }
        applyPageSizeOnItems(reqParams.getPage(), pageSize);
        applyPageSizeOnItems(reqParams.getParams(), pageSize);
        reqParams.setBody(setJsonPageSizeFields(reqParams.getBody(), pageSize));
    }

    private void applyPageSizeOnItems(List<DsApiReqParamsItem> items, int pageSize) {
        if (items == null || items.isEmpty()) {
            return;
        }
        String val = String.valueOf(pageSize);
        for (DsApiReqParamsItem item : items) {
            if (item == null || StringUtils.isEmpty(item.getName())) {
                continue;
            }
            if (isPageSizeKey(item.getName())) {
                item.setValue(val);
            }
        }
    }

    private String setJsonPageSizeFields(String body, int pageSize) {
        if (StringUtils.isEmpty(body)) {
            return body;
        }
        String trimmed = body.trim();
        if (!trimmed.startsWith("{")) {
            return body;
        }
        try {
            JSONObject obj = JSON.parseObject(trimmed);
            if (obj == null) {
                return body;
            }
            boolean changed = false;
            for (String key : new ArrayList<>(obj.keySet())) {
                if (isPageSizeKey(key)) {
                    Object old = obj.get(key);
                    if (old instanceof String) {
                        obj.put(key, String.valueOf(pageSize));
                    } else {
                        obj.put(key, pageSize);
                    }
                    changed = true;
                }
            }
            return changed ? obj.toJSONString() : body;
        } catch (Exception e) {
            return body;
        }
    }

    private boolean isPageSizeKey(String name) {
        if (StringUtils.isEmpty(name)) {
            return false;
        }
        String n = name.trim().toLowerCase(Locale.ROOT).replace("_", "");
        for (String k : PAGE_SIZE_KEYS) {
            if (k.equals(n)) {
                return true;
            }
        }
        return false;
    }

    private void setItemValueByName(List<DsApiReqParamsItem> items, String name, String value) {
        if (items == null || StringUtils.isEmpty(name)) {
            return;
        }
        for (DsApiReqParamsItem item : items) {
            if (item == null || StringUtils.isEmpty(item.getName())) {
                continue;
            }
            if (name.equalsIgnoreCase(item.getName().trim())) {
                item.setValue(value);
            }
        }
    }

    private String setJsonNumberOrStringField(String body, String field, int pageNum) {
        if (StringUtils.isEmpty(body) || StringUtils.isEmpty(field)) {
            return body;
        }
        String trimmed = body.trim();
        if (!trimmed.startsWith("{")) {
            return body;
        }
        try {
            JSONObject obj = JSON.parseObject(trimmed);
            if (obj == null || !obj.containsKey(field)) {
                // 尝试大小写不敏感匹配已有 key
                String matched = null;
                if (obj != null) {
                    for (String key : obj.keySet()) {
                        if (field.equalsIgnoreCase(key)) {
                            matched = key;
                            break;
                        }
                    }
                }
                if (matched == null) {
                    return body;
                }
                field = matched;
            }
            Object old = obj.get(field);
            if (old instanceof String) {
                obj.put(field, String.valueOf(pageNum));
            } else {
                obj.put(field, pageNum);
            }
            return obj.toJSONString();
        } catch (Exception e) {
            return body;
        }
    }

    private Object parseRootJson(String raw) {
        if (StringUtils.isEmpty(raw)) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.startsWith("[")) {
            return JSON.parseArray(trimmed);
        }
        return JSON.parseObject(trimmed);
    }

    private static final class RowExtract {
        private final String prefix;
        private final List<Object> items;

        private RowExtract(String prefix, List<Object> items) {
            this.prefix = prefix == null ? "" : prefix;
            this.items = items;
        }
    }

    /**
     * 从 API 响应中拆出「业务行」列表。
     * 优先识别 data / list / records 等对象数组，避免把整段 JSON 当成一行。
     */
    private RowExtract extractRowItems(Object root, Object parsed, String dataItemKey) {
        String configuredPrefix = normalizeDataItemKeyPrefix(dataItemKey);
        if (parsed instanceof JSONArray) {
            return new RowExtract(configuredPrefix, jsonArrayToList((JSONArray) parsed));
        }

        // 先在 dataItemKey 指向的节点上找对象数组（含 data.list 这类分页结构）
        Object search = parsed != null ? parsed : root;
        RowExtract found = findObjectArrayRows(search, configuredPrefix, 0);
        if (found != null && !found.items.isEmpty()) {
            return found;
        }
        // 再从根节点兜底查找（dataItemKey 配错或指向单对象时）
        if (root != null && root != search) {
            found = findObjectArrayRows(root, "", 0);
            if (found != null && !found.items.isEmpty()) {
                return found;
            }
        }
        if (parsed instanceof JSONObject) {
            List<Object> one = new ArrayList<>(1);
            one.add(parsed);
            return new RowExtract(configuredPrefix, one);
        }
        return new RowExtract(configuredPrefix, new ArrayList<>());
    }

    private String normalizeDataItemKeyPrefix(String dataItemKey) {
        if (StringUtils.isEmpty(dataItemKey)) {
            return "";
        }
        String key = dataItemKey.trim();
        if (key.startsWith("jsonObject.") || key.startsWith("jsonArray.") || key.startsWith("jsonSting.")) {
            int i = key.indexOf('.');
            key = i >= 0 ? key.substring(i + 1) : "";
        }
        return key;
    }

    private List<Object> jsonArrayToList(JSONArray arr) {
        List<Object> items = new ArrayList<>();
        if (arr == null) {
            return items;
        }
        for (int i = 0; i < arr.size(); i++) {
            items.add(arr.get(i));
        }
        return items;
    }

    private boolean isObjectArray(JSONArray arr) {
        if (arr == null || arr.isEmpty()) {
            return false;
        }
        Object first = arr.get(0);
        return first instanceof JSONObject || first instanceof Map;
    }

    /**
     * 递归查找「对象数组」作为同步行；常见路径优先：data/list/rows/records/...
     */
    private RowExtract findObjectArrayRows(Object node, String prefix, int depth) {
        if (node == null || depth > 4) {
            return null;
        }
        if (node instanceof JSONArray) {
            JSONArray arr = (JSONArray) node;
            if (isObjectArray(arr)) {
                return new RowExtract(prefix, jsonArrayToList(arr));
            }
            return null;
        }
        if (!(node instanceof JSONObject)) {
            return null;
        }
        JSONObject obj = (JSONObject) node;
        String[] preferred = new String[]{"data", "list", "rows", "records", "result", "items", "content"};
        for (String candidate : preferred) {
            if (!obj.containsKey(candidate)) {
                continue;
            }
            Object v = obj.get(candidate);
            String nextPrefix = StringUtils.isEmpty(prefix) ? candidate : prefix + "." + candidate;
            if (v instanceof JSONArray && isObjectArray((JSONArray) v)) {
                return new RowExtract(nextPrefix, jsonArrayToList((JSONArray) v));
            }
            if (v instanceof JSONObject) {
                RowExtract nested = findObjectArrayRows(v, nextPrefix, depth + 1);
                if (nested != null && !nested.items.isEmpty()) {
                    return nested;
                }
            }
        }
        // 兜底：扫描其余字段，取第一个对象数组
        for (String key : obj.keySet()) {
            Object v = obj.get(key);
            String nextPrefix = StringUtils.isEmpty(prefix) ? key : prefix + "." + key;
            if (v instanceof JSONArray && isObjectArray((JSONArray) v)) {
                return new RowExtract(nextPrefix, jsonArrayToList((JSONArray) v));
            }
        }
        for (String key : obj.keySet()) {
            Object v = obj.get(key);
            if (v instanceof JSONObject) {
                String nextPrefix = StringUtils.isEmpty(prefix) ? key : prefix + "." + key;
                RowExtract nested = findObjectArrayRows(v, nextPrefix, depth + 1);
                if (nested != null && !nested.items.isEmpty()) {
                    return nested;
                }
            }
        }
        return null;
    }

    /** 与 flattenFields 类似，但保留原始类型供入库推断列类型 */
    private void flattenValueFields(String prefix, Object node, Map<String, Object> out, int depth) {
        if (node == null || depth > 6) {
            return;
        }
        if (node instanceof JSONObject) {
            JSONObject obj = (JSONObject) node;
            for (String key : obj.keySet()) {
                Object val = obj.get(key);
                String path = StringUtils.isEmpty(prefix) ? key : prefix + "." + key;
                if (val instanceof JSONObject) {
                    flattenValueFields(path, val, out, depth + 1);
                } else if (val instanceof JSONArray) {
                    out.put(path, val == null ? null : String.valueOf(val));
                } else {
                    out.put(path, val);
                }
            }
        } else if (node instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) node;
            for (Map.Entry<String, Object> e : map.entrySet()) {
                Object val = e.getValue();
                String path = StringUtils.isEmpty(prefix) ? e.getKey() : prefix + "." + e.getKey();
                if (val instanceof Map || val instanceof JSONObject) {
                    flattenValueFields(path, val, out, depth + 1);
                } else {
                    out.put(path, val);
                }
            }
        } else if (StringUtils.isNotEmpty(prefix)) {
            out.put(prefix, node);
        }
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
        if (entity.getPageApi() == null) {
            entity.setPageApi(0);
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
            // 分页接口测试：缩小 pageSize，只打一页，避免响应过大导致页面卡顿
            DsApiTestResult result = doHttpRequest(entity, relyValues, null, isPageApi(entity));
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
        DsApiTestResult relyResult = doHttpRequest(rely, null, null, isPageApi(rely));
        if (relyResult.getTestStatus() == null || relyResult.getTestStatus() != 0) {
            throw new IllegalStateException("rely api call failed: " + relyResult.getTestResult());
        }
        Object parsed = parseByDataItemKey(rely.getDataItemKey(), relyResult.getTestResult());
        flattenFields("", parsed, map, 0);
        return map;
    }

    /**
     * @param pageNumOverride 非空时覆盖请求中的页码（pageNum/pageNo 等），用于作业自动翻页
     * @param shrinkPageSize  为 true 时把 pageSize/limit 等改为 {@link #TEST_PAGE_SIZE}（测试连接）
     */
    private DsApiTestResult doHttpRequest(DsApiDatasource entity, Map<String, String> relyValues,
                                          Integer pageNumOverride, boolean shrinkPageSize) {
        DsApiTestResult result = new DsApiTestResult();
        long begin = System.currentTimeMillis();
        try {
            fillReqParams(entity);
            DsApiReqParams reqParams = copyReqParams(entity.getReqParams());
            if (relyValues != null && !relyValues.isEmpty()) {
                replacePlaceholders(reqParams, relyValues);
            }
            if (shrinkPageSize) {
                applyPageSize(reqParams, TEST_PAGE_SIZE);
            }
            if (pageNumOverride != null) {
                String pageKey = detectPageNumKey(reqParams);
                if (StringUtils.isEmpty(pageKey)) {
                    pageKey = "pageNum";
                }
                applyPageNumber(reqParams, pageKey, pageNumOverride);
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
            if (result.getTestStatus() == 1) {
                String method = REQ_POST == reqType ? "POST" : "GET";
                String hint = "";
                if (code == 405) {
                    hint = REQ_POST == reqType
                            ? "；405=方法不允许，请改成 GET，并把 pageNum/pageSize 放到「入参/分页参数」而不是 Body"
                            : "；405=方法不允许，请改成 POST，并把 JSON 放到 Body";
                }
                if (StringUtils.isEmpty(bodyText)) {
                    result.setTestResult("HTTP " + code + " " + method + " " + url + hint);
                } else {
                    result.setTestResult("HTTP " + code + " " + method + " " + url + hint + "\n" + bodyText);
                }
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
