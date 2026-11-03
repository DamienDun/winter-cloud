package com.winter.cloud.common.filestorage;

import com.winter.cloud.common.core.exception.base.BaseException;
import com.winter.cloud.common.core.utils.ExceptionUtil;
import com.winter.cloud.common.core.utils.StringUtils;
import com.winter.cloud.common.core.utils.tuple.TupleTwo;
import com.winter.cloud.common.filestorage.properties.AbstractStorageClientProperties;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 存储客户端抽象
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2022/8/15 20:50
 */
public abstract class AbstractStorageClient<TBucket extends AbstractBucket> implements StorageClient {

    private static final Map<String, TupleTwo<String, Integer>> EXTENSION_CONTENT_TYPE_MAP = new HashMap<>(64);

    static {
        // 图片
        EXTENSION_CONTENT_TYPE_MAP.put("jpg", new TupleTwo<>("image/jpeg", 1));
        EXTENSION_CONTENT_TYPE_MAP.put("jpeg", new TupleTwo<>("image/jpeg", 1));
        EXTENSION_CONTENT_TYPE_MAP.put("png", new TupleTwo<>("image/png", 1));
        EXTENSION_CONTENT_TYPE_MAP.put("bmp", new TupleTwo<>("image/bmp", 1));
        EXTENSION_CONTENT_TYPE_MAP.put("gif", new TupleTwo<>("image/gif", 1));
        EXTENSION_CONTENT_TYPE_MAP.put("tif", new TupleTwo<>("image/tiff", 1));
        EXTENSION_CONTENT_TYPE_MAP.put("tiff", new TupleTwo<>("image/tiff", 1));
        EXTENSION_CONTENT_TYPE_MAP.put("dib", new TupleTwo<>("image/bmp", 1));
        EXTENSION_CONTENT_TYPE_MAP.put("webp", new TupleTwo<>("image/webp", 1));
        EXTENSION_CONTENT_TYPE_MAP.put("svg", new TupleTwo<>("image/svg+xml", 1));
        EXTENSION_CONTENT_TYPE_MAP.put("ico", new TupleTwo<>("image/x-icon", 1));
        EXTENSION_CONTENT_TYPE_MAP.put("heic", new TupleTwo<>("image/heic", 1));
        EXTENSION_CONTENT_TYPE_MAP.put("heif", new TupleTwo<>("image/heif", 1));
        // 文本 / 网页
        EXTENSION_CONTENT_TYPE_MAP.put("html", new TupleTwo<>("text/html", 2));
        EXTENSION_CONTENT_TYPE_MAP.put("htm", new TupleTwo<>("text/html", 2));
        EXTENSION_CONTENT_TYPE_MAP.put("xml", new TupleTwo<>("text/xml", 2));
        EXTENSION_CONTENT_TYPE_MAP.put("plain", new TupleTwo<>("text/plain", 2));
        EXTENSION_CONTENT_TYPE_MAP.put("txt", new TupleTwo<>("text/plain", 2));
        EXTENSION_CONTENT_TYPE_MAP.put("md", new TupleTwo<>("text/markdown", 2));
        EXTENSION_CONTENT_TYPE_MAP.put("csv", new TupleTwo<>("text/csv", 2));
        EXTENSION_CONTENT_TYPE_MAP.put("json", new TupleTwo<>("application/json", 3));
        EXTENSION_CONTENT_TYPE_MAP.put("pdf", new TupleTwo<>("application/pdf", 4));
        // 音视频（缺映射时会落成 application/octet-stream，叠加 nosniff 会导致播放器黑屏）
        EXTENSION_CONTENT_TYPE_MAP.put("mp4", new TupleTwo<>("video/mp4", 5));
        EXTENSION_CONTENT_TYPE_MAP.put("webm", new TupleTwo<>("video/webm", 5));
        EXTENSION_CONTENT_TYPE_MAP.put("ogg", new TupleTwo<>("video/ogg", 5));
        EXTENSION_CONTENT_TYPE_MAP.put("ogv", new TupleTwo<>("video/ogg", 5));
        EXTENSION_CONTENT_TYPE_MAP.put("mov", new TupleTwo<>("video/quicktime", 5));
        EXTENSION_CONTENT_TYPE_MAP.put("m4v", new TupleTwo<>("video/x-m4v", 5));
        EXTENSION_CONTENT_TYPE_MAP.put("avi", new TupleTwo<>("video/x-msvideo", 5));
        EXTENSION_CONTENT_TYPE_MAP.put("mkv", new TupleTwo<>("video/x-matroska", 5));
        EXTENSION_CONTENT_TYPE_MAP.put("flv", new TupleTwo<>("video/x-flv", 5));
        EXTENSION_CONTENT_TYPE_MAP.put("wmv", new TupleTwo<>("video/x-ms-wmv", 5));
        EXTENSION_CONTENT_TYPE_MAP.put("mpeg", new TupleTwo<>("video/mpeg", 5));
        EXTENSION_CONTENT_TYPE_MAP.put("mpg", new TupleTwo<>("video/mpeg", 5));
        EXTENSION_CONTENT_TYPE_MAP.put("3gp", new TupleTwo<>("video/3gpp", 5));
        EXTENSION_CONTENT_TYPE_MAP.put("ts", new TupleTwo<>("video/mp2t", 5));
        EXTENSION_CONTENT_TYPE_MAP.put("mp3", new TupleTwo<>("audio/mpeg", 6));
        EXTENSION_CONTENT_TYPE_MAP.put("wav", new TupleTwo<>("audio/wav", 6));
        EXTENSION_CONTENT_TYPE_MAP.put("flac", new TupleTwo<>("audio/flac", 6));
        EXTENSION_CONTENT_TYPE_MAP.put("aac", new TupleTwo<>("audio/aac", 6));
        EXTENSION_CONTENT_TYPE_MAP.put("m4a", new TupleTwo<>("audio/mp4", 6));
        EXTENSION_CONTENT_TYPE_MAP.put("wma", new TupleTwo<>("audio/x-ms-wma", 6));
        EXTENSION_CONTENT_TYPE_MAP.put("opus", new TupleTwo<>("audio/opus", 6));
        EXTENSION_CONTENT_TYPE_MAP.put("amr", new TupleTwo<>("audio/amr", 6));
        // 办公常见
        EXTENSION_CONTENT_TYPE_MAP.put("doc", new TupleTwo<>("application/msword", 7));
        EXTENSION_CONTENT_TYPE_MAP.put("docx", new TupleTwo<>("application/vnd.openxmlformats-officedocument.wordprocessingml.document", 7));
        EXTENSION_CONTENT_TYPE_MAP.put("xls", new TupleTwo<>("application/vnd.ms-excel", 7));
        EXTENSION_CONTENT_TYPE_MAP.put("xlsx", new TupleTwo<>("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", 7));
        EXTENSION_CONTENT_TYPE_MAP.put("ppt", new TupleTwo<>("application/vnd.ms-powerpoint", 7));
        EXTENSION_CONTENT_TYPE_MAP.put("pptx", new TupleTwo<>("application/vnd.openxmlformats-officedocument.presentationml.presentation", 7));
    }

    /**
     * 日志
     */
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * Url 分隔符
     */
    public static final char URL_SEPARATOR = '/';

    private final String endpoint;
    private final String defaultBucketName;
    private int writeBlockSize = 2048;
    private int readBlockSize = 4096;

    /**
     * AbstractStorageClient
     *
     * @param endpoint          终节点
     * @param defaultBucketName 默认分区
     */
    public AbstractStorageClient(String endpoint, String defaultBucketName) {
        if (StringUtils.isEmpty(endpoint)) {
            throw new BaseException("终节点(url根路径)不能为空。");
        }
        this.endpoint = StringUtils.removeStart(StringUtils.removeEnd(endpoint, String.valueOf(URL_SEPARATOR)), String.valueOf(URL_SEPARATOR))
                .toLowerCase();
        if (defaultBucketName == null) {
            defaultBucketName = "";
        } else {
            defaultBucketName = this.checkBucketName(defaultBucketName);
        }
        this.defaultBucketName = defaultBucketName.trim().toLowerCase();
    }

    /**
     * 实例化
     *
     * @param properties 属性
     */
    public AbstractStorageClient(AbstractStorageClientProperties properties) {
        this(properties.getEndpoint(), properties.getDefaultBucketName());
    }

    /**
     * 设置客户端属性
     *
     * @param properties 属性
     */
    protected void setStorageClientProperties(AbstractStorageClientProperties properties) {
        this.setReadBlockSize(properties.getReadBlockSize());
        this.setWriteBlockSize(properties.getWriteBlockSize());
    }


    @Override
    public String getEndpoint() {
        return this.endpoint;
    }

    @Override
    public String getDefaultBucketName() {
        return this.defaultBucketName;
    }

    @Override
    public abstract TBucket createBucket(String bucketName);

    @Override
    public abstract TBucket getBucket(String bucketName);

    /**
     * 获取日志
     *
     * @return
     */
    public final Logger getLogger() {
        return this.logger;
    }

    @Override
    public final FileObject saveFile(String bucketName, String fullPath, InputStream input) throws Exception {
        FileStorageRequest request = new FileStorageRequest(bucketName, fullPath, input);
        return this.saveFile(request);
    }

    @Override
    public final List<FileObject> listFileObjects(String bucketName) {
        return this.listFileObjects(bucketName, null);
    }

    /**
     * 检查分区名称
     *
     * @param bucketName 分区名称
     * @return
     */
    protected String checkBucketName(String bucketName) {
        if (StringUtils.isNotEmpty(bucketName)) {
            return bucketName;
        }
        return ExceptionUtil.checkNotNullOrBlank(this.getDefaultBucketName(), "bucketName");
    }

    /**
     * 获取路径地址
     *
     * @param args 参数集合
     * @return
     */
    protected final String getPathAddress(String... args) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (i > 0) {
                builder.append(URL_SEPARATOR);
            }
            builder.append(arg);
        }
        return builder.toString();
    }

    /**
     * 写入到输出流
     *
     * @param inputStream  输入流
     * @param outputStream 输出流
     * @return
     * @throws IOException
     */
    protected final long writeOutputStream(InputStream inputStream, OutputStream outputStream) throws IOException {
        long size = 0;
        int byteCount;
        byte[] bytes = new byte[this.getWriteBlockSize()];
        while ((byteCount = inputStream.read(bytes)) != -1) {
            outputStream.write(bytes, 0, byteCount);
            size += byteCount;
        }
        return size;
    }

    /**
     * 获取读取块大小
     *
     * @return
     */
    @Override
    public int getReadBlockSize() {
        return this.readBlockSize;
    }

    /**
     * 设置读块大小
     *
     * @param readBlockSize 读块大小
     */
    @Override
    public void setReadBlockSize(int readBlockSize) {
        this.readBlockSize = readBlockSize;
    }

    /**
     * 获取写块大小
     *
     * @return
     */
    @Override
    public int getWriteBlockSize() {
        return this.writeBlockSize;
    }

    /**
     * 设置写块大小
     *
     * @param writeBlockSize 写块大小
     */
    @Override
    public void setWriteBlockSize(int writeBlockSize) {
        this.writeBlockSize = writeBlockSize;
    }

    @Override
    public String toString() {
        return "启用 FileStorage " + this.getChannelName() + " 默认分区:" + this.getDefaultBucketName() + " 访问根路径:"
                + this.getEndpoint();
    }

    /**
     * 获取文件
     *
     * @param ext 扩展名
     * @return
     */
    public String getFileContentType(String ext) {
        if (ext == null) {
            return "application/octet-stream";
        }
        TupleTwo<String, Integer> two = EXTENSION_CONTENT_TYPE_MAP.get(ext.toLowerCase().trim());
        if (two != null) {
            return two.getItem1();
        }
        return "application/octet-stream";
    }

    public static final String CONTENT_DISPOSITION = "Content-Disposition";

    /**
     * 获取内容 Disposition
     *
     * @param request
     * @return
     */
    protected String getContentDisposition(FileStorageRequest request) {
        if (StringUtils.isNotEmpty(request.getFileInfo().getFriendlyName())) {
            try {
                String fileName = request.getFileInfo().getFriendlyName() + "." + request.getFileInfo().getExtensionName();
                return "attachment;filename=" + URLEncoder.encode(fileName, StandardCharsets.UTF_8.name());
            } catch (UnsupportedEncodingException e) {
                return "";
            }
        }
        return "";
    }

    /**
     * 读取请求流
     *
     * @param request 请求
     * @return
     * @throws IOException
     */
    protected TupleTwo<Long, InputStream> readRequestStream(FileStorageRequest request) throws IOException {
        InputStream inputStream;
        long size;
        if (request.getFileInfo().getLength() > 0L) {
            size = request.getFileInfo().getLength();
            inputStream = request.getInputStream();
        } else {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            try {
                size = this.writeOutputStream(request.getInputStream(), outputStream);
                if (request.getInputStream() instanceof ByteArrayInputStream) {
                    ByteArrayInputStream byteArrayInputStream = (ByteArrayInputStream) request.getInputStream();
                    byteArrayInputStream.reset();
                    inputStream = byteArrayInputStream;
                } else {
                    inputStream = new ByteArrayInputStream(outputStream.toByteArray());
                }
            } finally {
                IOUtils.closeQuietly(outputStream);
            }
        }
        return new TupleTwo<>(size, inputStream);
    }
}
