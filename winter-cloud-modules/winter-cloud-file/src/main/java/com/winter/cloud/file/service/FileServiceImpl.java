package com.winter.cloud.file.service;

import com.winter.cloud.common.core.utils.StringUtils;
import com.winter.cloud.common.core.utils.file.FileUtils;
import com.winter.cloud.common.core.utils.uuid.IdUtils;
import com.winter.cloud.common.filestorage.FileObject;
import com.winter.cloud.common.filestorage.FileStorageObject;
import com.winter.cloud.common.filestorage.StorageClient;
import com.winter.cloud.common.filestorage.StorageClientContext;
import com.winter.cloud.common.filestorage.properties.WinterStorageProperties;
import com.winter.cloud.file.domain.ResourceFile;
import com.winter.cloud.file.domain.dto.FileDownloadZipInput;
import com.winter.cloud.file.domain.dto.FileDownloadZipSpecifyNameInput;
import com.winter.cloud.file.domain.dto.FileNameVo;
import com.winter.cloud.file.utils.FileUploadUtils;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 统一文件存储实现：按配置选用 StorageClient，并写入 fs_resource_file 上传记录
 */
@Primary
@Service
public class FileServiceImpl implements IFileService {

    private static final Logger log = LoggerFactory.getLogger(FileServiceImpl.class);

    private final StorageClientContext storageClientContext;
    private final WinterStorageProperties storageProperties;
    private final IResourceFileService resourceFileService;

    public FileServiceImpl(StorageClientContext storageClientContext,
                                     WinterStorageProperties storageProperties,
                                     IResourceFileService resourceFileService) {
        this.storageClientContext = storageClientContext;
        this.storageProperties = storageProperties;
        this.resourceFileService = resourceFileService;
    }

    @Override
    public String uploadFile(MultipartFile file) throws Exception {
        StorageClient client = resolveClient();
        String fileMd5;
        try (InputStream md5Stream = file.getInputStream()) {
            fileMd5 = DigestUtils.md5Hex(md5Stream);
        }

        // 相同 MD5：累加上传次数，直接返回已有访问地址，避免重复写对象存储
        ResourceFile exist = resourceFileService.queryByFileMd5(fileMd5);
        if (exist != null && StringUtils.isNotEmpty(exist.getPath())) {
            resourceFileService.saveUploadRecord(file, exist.getPath(), exist.getRelativePath(),
                    client.getChannelId(), client.getDefaultBucketName(), fileMd5, exist.getLength());
            return exist.getPath();
        }

        String objectName = FileUploadUtils.extractFilename(file);
        try (InputStream inputStream = file.getInputStream()) {
            FileObject fileObject = client.saveFile(client.getDefaultBucketName(), objectName, inputStream);
            log.info("文件上传成功[channel={}]: {}", client.getChannelId(), fileObject);
            String relativePath = fileObject.getUrl();
            Long length = fileObject.getFileInfo() == null ? file.getSize() : fileObject.getFileInfo().getLength();
            resourceFileService.saveUploadRecord(file, fileObject.getAccessUrl(), relativePath,
                    client.getChannelId(), client.getDefaultBucketName(), fileMd5, length);
            return fileObject.getAccessUrl();
        }
    }

    @Override
    public void deleteFile(String fileUrl) throws Exception {
        StorageClient client = resolveClient();
        String objectPath = resolveObjectPath(client, fileUrl);
        client.deleteFile(client.getDefaultBucketName(), objectPath);
        resourceFileService.deleteByPath(fileUrl);
        if (!objectPath.equals(fileUrl)) {
            resourceFileService.deleteByPath(objectPath);
        }
    }

    @Override
    public boolean existFile(String path) {
        if (StringUtils.isEmpty(path)) {
            return false;
        }
        try {
            StorageClient client = resolveClient();
            String objectPath = resolveObjectPath(client, path);
            return client.existFile(client.getDefaultBucketName(), objectPath);
        } catch (Exception e) {
            log.warn("检查文件是否存在失败: {}", path, e);
            return false;
        }
    }

    @Override
    public void downloadFile(String path, HttpServletResponse response) throws Exception {
        StorageClient client = resolveClient();
        String objectPath = resolveObjectPath(client, path);
        FileStorageObject storageObject = client.getFile(client.getDefaultBucketName(), objectPath);
        if (storageObject == null || storageObject.getInputStream() == null) {
            throw new RuntimeException("文件不存在: " + path);
        }
        String downloadName = FileUtils.getName(objectPath);
        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        FileUtils.setAttachmentResponseHeader(response, downloadName);
        try (InputStream inputStream = storageObject.getInputStream()) {
            IOUtils.copy(inputStream, response.getOutputStream());
            response.flushBuffer();
        }
    }

    @Override
    public void downloadZip(FileDownloadZipInput input, HttpServletResponse response) {
        if (input == null || CollectionUtils.isEmpty(input.getUrls())) {
            return;
        }
        // 去重，保留首次出现顺序
        Map<String, String> entryNameByUrl = new LinkedHashMap<>();
        for (String url : input.getUrls()) {
            if (StringUtils.isEmpty(url) || entryNameByUrl.containsKey(url)) {
                continue;
            }
            entryNameByUrl.put(url, FileUtils.getName(url.replace("\\", "/")));
        }
        writeZipResponse(entryNameByUrl, response);
    }

    @Override
    public void downloadZipSpecifyName(FileDownloadZipSpecifyNameInput input, HttpServletResponse response) {
        if (input == null || CollectionUtils.isEmpty(input.getUrls())) {
            return;
        }
        Map<String, String> entryNameByUrl = new LinkedHashMap<>();
        for (FileNameVo vo : input.getUrls()) {
            if (vo == null || StringUtils.isEmpty(vo.getUrl()) || entryNameByUrl.containsKey(vo.getUrl())) {
                continue;
            }
            String entryName = StringUtils.isNotEmpty(vo.getSpecifyFileName())
                    ? vo.getSpecifyFileName()
                    : FileUtils.getName(vo.getUrl().replace("\\", "/"));
            entryNameByUrl.put(vo.getUrl(), entryName);
        }
        writeZipResponse(entryNameByUrl, response);
    }

    /**
     * 从存储读取多个对象，边读边写入 zip 响应流（不落临时 zip 文件）
     */
    private void writeZipResponse(Map<String, String> entryNameByUrl, HttpServletResponse response) {
        if (entryNameByUrl.isEmpty()) {
            return;
        }
        String zipFileName = IdUtils.fastSimpleUUID() + ".zip";
        StorageClient client = resolveClient();
        Set<String> usedEntryNames = new HashSet<>();
        try {
            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            FileUtils.setAttachmentResponseHeader(response, zipFileName);
            try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream())) {
                for (Map.Entry<String, String> item : entryNameByUrl.entrySet()) {
                    String url = item.getKey();
                    String entryName = uniqueEntryName(item.getValue(), usedEntryNames);
                    try {
                        String objectPath = resolveObjectPath(client, url);
                        FileStorageObject storageObject = client.getFile(client.getDefaultBucketName(), objectPath);
                        if (storageObject == null || storageObject.getInputStream() == null) {
                            log.warn("打包跳过，文件不存在: {}", url);
                            continue;
                        }
                        zos.putNextEntry(new ZipEntry(entryName));
                        try (InputStream inputStream = storageObject.getInputStream()) {
                            IOUtils.copy(inputStream, zos);
                        }
                        zos.closeEntry();
                    } catch (Exception e) {
                        log.error("单个文件打包失败: {}", url, e);
                    }
                }
                zos.finish();
            }
            response.flushBuffer();
        } catch (Exception e) {
            log.error("下载zip文件失败", e);
        }
    }

    private String uniqueEntryName(String preferred, Set<String> used) {
        String name = StringUtils.isEmpty(preferred) ? "file" : preferred.replace("\\", "/");
        // 去掉路径前缀，只保留文件名，避免 zip 内出现目录穿越
        name = FileUtils.getName(name);
        if (StringUtils.isEmpty(name)) {
            name = "file";
        }
        String candidate = name;
        int index = 1;
        while (!used.add(candidate)) {
            int dot = name.lastIndexOf('.');
            if (dot > 0) {
                candidate = name.substring(0, dot) + "_" + index + name.substring(dot);
            } else {
                candidate = name + "_" + index;
            }
            index++;
        }
        return candidate;
    }

    private StorageClient resolveClient() {
        String channelId = storageProperties.getDefaultChannel();
        if (StringUtils.isEmpty(channelId)) {
            channelId = storageProperties.getMultipartChannel();
        }
        if (StringUtils.isNotEmpty(channelId)) {
            StorageClient client = storageClientContext.getChannel(channelId);
            if (client == null) {
                throw new IllegalStateException("未找到存储通道[" + channelId + "]，请检查 winter.storage.client 配置");
            }
            return client;
        }
        Collection<StorageClient> channels = storageClientContext.channels();
        if (channels == null || channels.isEmpty()) {
            throw new IllegalStateException("未启用任何存储客户端，请配置 winter.storage.client.*.enable=true");
        }
        if (channels.size() > 1) {
            throw new IllegalStateException("启用了多个存储客户端，请配置 winter.storage.client.default-channel");
        }
        return channels.iterator().next();
    }

    private String resolveObjectPath(StorageClient client, String fileUrl) {
        if (StringUtils.isEmpty(fileUrl)) {
            throw new IllegalArgumentException("文件路径不能为空");
        }
        String path = fileUrl.trim().replace("\\", "/");
        String bucket = client.getDefaultBucketName();
        String bucketToken = "/" + bucket + "/";
        int idx = path.indexOf(bucketToken);
        if (idx >= 0) {
            return StringUtils.removeStart(path.substring(idx + bucketToken.length()), "/");
        }
        if (StringUtils.isNotEmpty(bucket) && path.startsWith(bucket + "/")) {
            return path.substring(bucket.length() + 1);
        }
        return StringUtils.removeStart(path, "/");
    }
}