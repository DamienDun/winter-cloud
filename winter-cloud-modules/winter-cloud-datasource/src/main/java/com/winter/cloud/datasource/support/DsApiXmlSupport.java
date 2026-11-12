package com.winter.cloud.datasource.support;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.winter.cloud.common.core.utils.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * WebService/SOAP 响应 XML → JSON 树（按 localName，忽略命名空间），便于复用现有 JSON 路径解析。
 */
public final class DsApiXmlSupport {

    private DsApiXmlSupport() {
    }

    public static boolean looksLikeXml(String raw) {
        if (StringUtils.isEmpty(raw)) {
            return false;
        }
        String t = raw.trim();
        return t.startsWith("<") && !t.startsWith("<!");
    }

    /**
     * 解析 XML 为 Fastjson 结构；根节点以 localName 包一层，例如 Envelope → {Envelope:{...}}。
     */
    public static Object parseToJson(String xml) {
        if (StringUtils.isEmpty(xml)) {
            return null;
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setExpandEntityReferences(false);
            try {
                factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
                factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
                factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            } catch (Exception ignore) {
                // 部分 JDK 不支持全部 feature，忽略
            }
            Document doc = factory.newDocumentBuilder()
                    .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
            Element root = doc.getDocumentElement();
            if (root == null) {
                return null;
            }
            Object body = elementToJson(root);
            // 用 LinkedHashMap 保序（当前 fastjson2 无 JSONObject(boolean) 构造器）
            JSONObject wrap = new JSONObject(new LinkedHashMap<>());
            wrap.put(localName(root), body);
            return wrap;
        } catch (Exception e) {
            throw new IllegalArgumentException("XML 解析失败: " + e.getMessage(), e);
        }
    }

    /**
     * 按点路径取值（忽略命名空间前缀），如 Envelope.Body.getListResponse.return
     */
    public static Object navigate(Object root, String path) {
        if (root == null) {
            return null;
        }
        if (StringUtils.isEmpty(path)) {
            return root;
        }
        Object cur = root;
        for (String rawPart : path.split("\\.")) {
            if (cur == null) {
                return null;
            }
            String part = stripPrefix(rawPart);
            if (StringUtils.isEmpty(part)) {
                continue;
            }
            Matcher m = Pattern.compile("^(.*)\\[(\\d+)\\]$").matcher(part);
            if (m.find()) {
                String arrKey = stripPrefix(m.group(1));
                int idx = Integer.parseInt(m.group(2));
                cur = child(cur, arrKey);
                if (cur instanceof JSONArray) {
                    JSONArray arr = (JSONArray) cur;
                    cur = idx >= 0 && idx < arr.size() ? arr.get(idx) : null;
                } else if (cur instanceof List) {
                    List<?> list = (List<?>) cur;
                    cur = idx >= 0 && idx < list.size() ? list.get(idx) : null;
                } else {
                    return null;
                }
            } else {
                cur = child(cur, part);
            }
        }
        return cur;
    }

    /**
     * 替换 XML 中指定 localName 标签的文本内容（用于分页 pageNum/pageSize）。
     */
    public static String replaceTagText(String xml, String tagLocalName, String value) {
        if (StringUtils.isEmpty(xml) || StringUtils.isEmpty(tagLocalName)) {
            return xml;
        }
        Pattern p = Pattern.compile(
                "(<(?:[\\w.-]+:)?" + Pattern.quote(tagLocalName) + ")(\\s[^>]*)?>([^<]*)(</(?:[\\w.-]+:)?"
                        + Pattern.quote(tagLocalName) + ">)",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher m = p.matcher(xml);
        StringBuffer sb = new StringBuffer();
        boolean found = false;
        while (m.find()) {
            found = true;
            String replacement = m.group(1) + (m.group(2) == null ? "" : m.group(2)) + ">"
                    + (value == null ? "" : value) + m.group(4);
            m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        if (!found) {
            return xml;
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /** 在 XML 中探测是否存在指定 localName 标签 */
    public static boolean containsTag(String xml, String tagLocalName) {
        if (StringUtils.isEmpty(xml) || StringUtils.isEmpty(tagLocalName)) {
            return false;
        }
        Pattern p = Pattern.compile(
                "<(?:[\\w.-]+:)?" + Pattern.quote(tagLocalName) + "(?:\\s|>|/)",
                Pattern.CASE_INSENSITIVE);
        return p.matcher(xml).find();
    }

    private static Object elementToJson(Element el) {
        NodeList nodes = el.getChildNodes();
        Map<String, List<Object>> groups = new LinkedHashMap<>();
        StringBuilder text = new StringBuilder();
        boolean hasElementChild = false;
        for (int i = 0; i < nodes.getLength(); i++) {
            Node n = nodes.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                hasElementChild = true;
                Element child = (Element) n;
                String name = localName(child);
                groups.computeIfAbsent(name, k -> new ArrayList<>()).add(elementToJson(child));
            } else if (n.getNodeType() == Node.TEXT_NODE || n.getNodeType() == Node.CDATA_SECTION_NODE) {
                String t = n.getTextContent();
                if (t != null && !t.trim().isEmpty()) {
                    text.append(t);
                }
            }
        }
        if (!hasElementChild) {
            return text.toString().trim();
        }
        JSONObject obj = new JSONObject(new LinkedHashMap<>());
        for (Map.Entry<String, List<Object>> e : groups.entrySet()) {
            List<Object> list = e.getValue();
            if (list.size() == 1) {
                obj.put(e.getKey(), list.get(0));
            } else {
                JSONArray arr = new JSONArray(list.size());
                for (Object item : list) {
                    arr.add(item);
                }
                obj.put(e.getKey(), arr);
            }
        }
        return obj;
    }

    private static Object child(Object cur, String key) {
        if (cur instanceof JSONObject) {
            JSONObject obj = (JSONObject) cur;
            if (obj.containsKey(key)) {
                return obj.get(key);
            }
            // 忽略大小写兜底
            for (String k : obj.keySet()) {
                if (key.equalsIgnoreCase(k)) {
                    return obj.get(k);
                }
            }
            return null;
        }
        if (cur instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) cur;
            if (map.containsKey(key)) {
                return map.get(key);
            }
            for (Map.Entry<String, Object> e : map.entrySet()) {
                if (key.equalsIgnoreCase(e.getKey())) {
                    return e.getValue();
                }
            }
        }
        return null;
    }

    private static String localName(Node node) {
        String ln = node.getLocalName();
        if (StringUtils.isNotEmpty(ln)) {
            return ln;
        }
        return stripPrefix(node.getNodeName());
    }

    private static String stripPrefix(String name) {
        if (StringUtils.isEmpty(name)) {
            return "";
        }
        String n = name.trim();
        int i = n.indexOf(':');
        return i >= 0 ? n.substring(i + 1) : n;
    }

    public static String normalizeDatasourceType(String datasourceType) {
        if (StringUtils.isEmpty(datasourceType)) {
            return "HTTP";
        }
        String t = datasourceType.trim().toUpperCase(Locale.ROOT);
        if ("WEBSERVICE".equals(t) || "WEB_SERVICE".equals(t) || "SOAP".equals(t) || "WS".equals(t)) {
            return "WEBSERVICE";
        }
        return "HTTP";
    }
}
