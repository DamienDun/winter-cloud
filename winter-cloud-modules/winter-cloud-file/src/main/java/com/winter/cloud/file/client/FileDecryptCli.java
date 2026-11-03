package com.winter.cloud.file.client;

import com.winter.cloud.common.core.config.WinterConfig;
import com.winter.cloud.common.core.exception.ServiceException;
import com.winter.cloud.common.core.utils.StringUtils;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

/**
 * 文件解密客户端（对齐 portal FileDecryptCli，调用 sec-server 解密接口）
 * <p>依赖 OkHttp 3.x（与 MinIO 一致），RequestBody.create(MediaType, byte[]) 参数顺序勿用 4.x 写法。
 */
@Component
@ConfigurationProperties(prefix = WinterConfig.PREFIX)
public class FileDecryptCli {

    private static final Logger log = LoggerFactory.getLogger(FileDecryptCli.class);

    private static final MediaType OCTET_STREAM = MediaType.parse("application/octet-stream");

    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .build();

    /**
     * 文件解密服务地址，配置项：winter.fileDecryptUrl
     */
    private String fileDecryptUrl;

    public String getFileDecryptUrl() {
        return fileDecryptUrl;
    }

    public void setFileDecryptUrl(String fileDecryptUrl) {
        this.fileDecryptUrl = fileDecryptUrl;
    }

    /**
     * 解密上传文件
     *
     * @return 解密后的流；returnFlag=2 时返回原文件流
     */
    public InputStream decrypt(MultipartFile file) {
        if (StringUtils.isEmpty(fileDecryptUrl)) {
            throw new ServiceException("未配置 winter.fileDecryptUrl，无法解密文件");
        }
        try {
            byte[] fileBytes = file.getBytes();
            // OkHttp 3.x: create(MediaType, byte[])；OkHttp 4.x 参数顺序相反
            RequestBody body = RequestBody.create(OCTET_STREAM, fileBytes);
            Request request = new Request.Builder()
                    .url(fileDecryptUrl)
                    .header("method~name", "fileDecryptionRest")
                    .header("data~fileOffset", "0")
                    .header("data~counSize", String.valueOf(file.getSize()))
                    .post(body)
                    .build();
            return doDecrypt(request, file);
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("===>>>文件解密失败", e);
            throw new ServiceException("文件解密失败");
        }
    }

    private InputStream doDecrypt(Request request, MultipartFile file) throws Exception {
        try (Response response = HTTP_CLIENT.newCall(request).execute()) {
            String result = response.header("data~returnFlag");
            log.info("文件解密结果：{}", result);
            if ("1".equals(result)) {
                throw new ServiceException("文件解密失败");
            }
            // 2：无需解密，使用原文件
            if ("2".equals(result)) {
                return file.getInputStream();
            }
            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                throw new ServiceException("文件解密失败");
            }
            // 必须读成字节再返回，避免 Response 关闭后流失效
            return new ByteArrayInputStream(responseBody.bytes());
        }
    }
}
