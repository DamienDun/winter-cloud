package com.winter.cloud.file.controller;

import com.winter.cloud.common.core.domain.R;
import com.winter.cloud.common.core.utils.StringUtils;
import com.winter.cloud.common.core.utils.file.FileUtils;
import com.winter.cloud.common.core.web.domain.AjaxResult;
import com.winter.cloud.file.domain.dto.FileDownloadZipInput;
import com.winter.cloud.file.domain.dto.FileDownloadZipSpecifyNameInput;
import com.winter.cloud.file.service.IFileService;
import com.winter.cloud.system.api.domain.SysFile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;

/**
 * 文件请求处理（上传/下载/删除），统一走配置选定的存储实现
 *
 * @author winter
 */
@Tag(name = "文件服务")
@RestController
public class FileController {
    private static final Logger log = LoggerFactory.getLogger(FileController.class);

    private static final String FILE_DELIMITER = ",";

    @Autowired
    private IFileService fileService;

    /**
     * 文件上传请求
     */
    @Operation(summary = "上传文件")
    @PostMapping("upload")
    public R<SysFile> upload(MultipartFile file) {
        try {
            // 上传并返回访问地址
            String url = fileService.uploadFile(file);
            SysFile sysFile = new SysFile();
            sysFile.setName(FileUtils.getName(url));
            sysFile.setUrl(url);
            return R.ok(sysFile);
        } catch (Exception e) {
            log.error("上传文件失败", e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 批量上传
     */
    @Operation(summary = "批量上传文件")
    @PostMapping("uploads")
    public AjaxResult uploads(List<MultipartFile> files) {
        try {
            List<String> urls = new ArrayList<>();
            List<String> fileNames = new ArrayList<>();
            List<String> newFileNames = new ArrayList<>();
            List<String> originalFilenames = new ArrayList<>();
            for (MultipartFile file : files) {
                String url = fileService.uploadFile(file);
                urls.add(url);
                fileNames.add(url);
                newFileNames.add(FileUtils.getName(url));
                originalFilenames.add(file.getOriginalFilename());
            }
            AjaxResult ajax = AjaxResult.success();
            ajax.put("urls", StringUtils.join(urls, FILE_DELIMITER));
            ajax.put("fileNames", StringUtils.join(fileNames, FILE_DELIMITER));
            ajax.put("newFileNames", StringUtils.join(newFileNames, FILE_DELIMITER));
            ajax.put("originalFilenames", StringUtils.join(originalFilenames, FILE_DELIMITER));
            return ajax;
        } catch (Exception e) {
            log.error("批量文件上传失败", e);
            return AjaxResult.error(e.getMessage());
        }
    }

    /**
     * 文件删除（Feign / RemoteFileService）
     */
    @Operation(summary = "删除文件")
    @DeleteMapping("delete")
    public R<Boolean> delete(String fileUrl) {
        try {
            if (!FileUtils.validateFilePath(fileUrl)) {
                throw new Exception(StringUtils.format("资源文件({})非法，不允许删除。 ", fileUrl));
            }
            fileService.deleteFile(fileUrl);
            return R.ok();
        } catch (Exception e) {
            log.error("删除文件失败", e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 下载文件（由配置的存储实现处理）
     *
     * @param path   对象路径或访问 URL
     * @param delete 下载后是否删除
     */
    @Operation(summary = "下载文件")
    @GetMapping("download")
    public void download(String path, Boolean delete, HttpServletResponse response) {
        try {
            if (StringUtils.isEmpty(path)) {
                throw new Exception("文件路径不能为空");
            }
            if (!fileService.existFile(path)) {
                throw new Exception(StringUtils.format("文件({})不存在。 ", path));
            }
            fileService.downloadFile(path, response);
            if (Boolean.TRUE.equals(delete)) {
                fileService.deleteFile(path);
            }
        } catch (Exception e) {
            log.error("下载文件失败", e);
        }
    }

    /**
     * 资源下载（同 download，统一走配置的存储实现）
     */
    @Operation(summary = "资源下载")
    @GetMapping("download/resource")
    public void downloadResource(String resource, HttpServletResponse response) {
        try {
            if (StringUtils.isEmpty(resource)) {
                throw new Exception("资源路径不能为空");
            }
            if (!fileService.existFile(resource)) {
                throw new Exception(StringUtils.format("资源文件({})不存在。 ", resource));
            }
            fileService.downloadFile(resource, response);
        } catch (Exception e) {
            log.error("下载文件失败", e);
        }
    }

    /**
     * 多个文件打包下载 zip（对齐 portal CommonController#downloadZip）
     */
    @Operation(summary = "多个文件打包下载zip")
    @PostMapping("downloadZip")
    public void downloadZip(@RequestBody @Valid FileDownloadZipInput input, HttpServletResponse response) {
        fileService.downloadZip(input, response);
    }

    /**
     * 指定 zip 内文件名的多文件打包下载（对齐 portal CommonController#downloadZipSpecifyName）
     */
    @Operation(summary = "指定名称,多个文件打包下载zip")
    @PostMapping("downloadZipSpecifyName")
    public void downloadZipSpecifyName(@RequestBody @Valid FileDownloadZipSpecifyNameInput input,
                                       HttpServletResponse response) {
        fileService.downloadZipSpecifyName(input, response);
    }
}
