package com.winter.cloud.common.web.util;

import com.winter.cloud.common.core.utils.StringUtils;
import com.winter.cloud.common.web.dto.DownloadFileInput;
import org.apache.commons.io.IOUtils;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2023/12/13 9:15
 */
public class DownloadUtils {

    /**
     * 获取 ContentType
     *
     * @param request     请求
     * @param fileName    完整文件名称
     * @param contentType 内容类型
     * @return
     */
    public static String getContentType(HttpServletRequest request, String fileName, String contentType) {
        ServletContext context = request.getServletContext();
        String mimeType;
        if (StringUtils.isEmpty(contentType)) {
            mimeType = context.getMimeType(fileName);
            if (StringUtils.isEmpty(mimeType)) {
                mimeType = "application/octet-stream";
            }
        } else {
            mimeType = contentType;
        }
        return mimeType;
    }

    /**
     * 断点继传下载
     *
     * @param input     输入
     * @param request   请求
     * @param response  响应
     * @param blockSize 块大小
     * @throws IOException
     */
    public static void download(DownloadFileInput input, HttpServletRequest request, HttpServletResponse response, int blockSize) throws IOException {
        String mimeType = getContentType(request, input.getFileName(), input.getContentType());
        response.setContentType(mimeType);
        ControllerUtils.setResponseFileName(response, input.getFileName());
        response.setHeader("Accept-Ranges", "bytes");
        if (blockSize <= 0) {
            blockSize = 4096;
        }
        long downloadSize = input.getFileSize();
        long fromPos = 0, toPos = 0;
        if (StringUtils.isEmpty(request.getHeader("Range"))) {
            response.setHeader("Content-Length", downloadSize + "");
        } else {
            response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
            String range = request.getHeader("Range");
            String bytes = range.replaceAll("bytes=", "");
            String[] ary = bytes.split("-");
            fromPos = Long.parseLong(ary[0]);
            if (ary.length == 2) {
                toPos = Long.parseLong(ary[1]);
            }
            long size;
            if (toPos > fromPos) {
                size = toPos - fromPos;
            } else {
                size = downloadSize - fromPos;
            }
            response.setHeader("Content-Length", size + "");
            downloadSize = size;
        }
        InputStream fileStream = input.getFileStream();
        OutputStream out = null;
        try {
            if (fromPos > 0) {
                fileStream.skip(fromPos);
            }
            int bufLen = (int) (downloadSize < blockSize ? downloadSize : blockSize);
            byte[] buffer = new byte[bufLen];
            int num;
            long count = 0;
            out = response.getOutputStream();
            while ((num = fileStream.read(buffer)) != -1) {
                out.write(buffer, 0, num);
                count += num;
                if (downloadSize - count < bufLen) {
                    bufLen = (int) (downloadSize - count);
                    if (bufLen == 0) {
                        break;
                    }
                    buffer = new byte[bufLen];
                }
            }
            response.flushBuffer();
        } finally {
            IOUtils.closeQuietly(fileStream);
            IOUtils.closeQuietly(out);
        }
    }
}
