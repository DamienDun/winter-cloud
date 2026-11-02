package com.winter.cloud.file.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.winter.cloud.common.core.utils.StringUtils;
import com.winter.cloud.common.core.utils.file.FileTypeUtils;
import com.winter.cloud.file.domain.entity.ResourceFile;
import com.winter.cloud.file.enums.ResourceFileTypeEnum;
import com.winter.cloud.file.mapper.ResourceFileMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 资源文件上传记录服务
 */
@Service
public class ResourceFileServiceImpl implements IResourceFileService {

    private final ResourceFileMapper resourceFileMapper;

    public ResourceFileServiceImpl(ResourceFileMapper resourceFileMapper) {
        this.resourceFileMapper = resourceFileMapper;
    }

    @Override
    public List<ResourceFile> selectResourceFileList(ResourceFile query) {
        LambdaQueryWrapper<ResourceFile> wrapper = Wrappers.lambdaQuery(ResourceFile.class);
        if (query != null) {
            wrapper.like(StringUtils.isNotEmpty(query.getOriginName()), ResourceFile::getOriginName, query.getOriginName())
                .eq(StringUtils.isNotEmpty(query.getFileMd5()), ResourceFile::getFileMd5, query.getFileMd5())
                .eq(StringUtils.isNotEmpty(query.getChannelId()), ResourceFile::getChannelId, query.getChannelId())
                .eq(StringUtils.isNotEmpty(query.getBucketName()), ResourceFile::getBucketName, query.getBucketName())
                .eq(StringUtils.isNotEmpty(query.getModule()), ResourceFile::getModule, query.getModule())
                .eq(query.getFileType() != null, ResourceFile::getFileType, query.getFileType())
                .like(StringUtils.isNotEmpty(query.getPath()), ResourceFile::getPath, query.getPath())
                .like(StringUtils.isNotEmpty(query.getRelativePath()), ResourceFile::getRelativePath, query.getRelativePath());
        }
        wrapper.orderByDesc(ResourceFile::getGmtCreate);
        return resourceFileMapper.selectList(wrapper);
    }

    @Override
    public ResourceFile selectById(Long id) {
        if (id == null) {
            return null;
        }
        return resourceFileMapper.selectById(id);
    }

    @Override
    public ResourceFile queryByFileMd5(String fileMd5) {
        if (StringUtils.isEmpty(fileMd5)) {
            return null;
        }
        LambdaQueryWrapper<ResourceFile> wrapper = Wrappers.lambdaQuery(ResourceFile.class);
        wrapper.eq(ResourceFile::getFileMd5, fileMd5).orderByAsc(ResourceFile::getGmtCreate).last("LIMIT 1");
        return resourceFileMapper.selectOne(wrapper);
    }

    @Override
    public ResourceFile queryByPath(String path) {
        if (StringUtils.isEmpty(path)) {
            return null;
        }
        LambdaQueryWrapper<ResourceFile> wrapper = Wrappers.lambdaQuery(ResourceFile.class);
        wrapper.eq(ResourceFile::getPath, path).last("LIMIT 1");
        return resourceFileMapper.selectOne(wrapper);
    }

    @Override
    public ResourceFile saveUploadRecord(MultipartFile file, String accessUrl, String relativePath,
                                         String channelId, String bucketName, String fileMd5, Long length) {
        if (StringUtils.isNotEmpty(fileMd5)) {
            ResourceFile exist = queryByFileMd5(fileMd5);
            if (exist != null) {
                Integer count = exist.getApplyUploadCount() == null ? 1 : exist.getApplyUploadCount();
                exist.setApplyUploadCount(count + 1);
                if (file != null && StringUtils.isNotEmpty(file.getOriginalFilename())) {
                    exist.setOriginName(file.getOriginalFilename());
                }
                // gmtModified / modifiedUserId 由 MetaObjectHandler 填充
                resourceFileMapper.updateById(exist);
                return exist;
            }
        }

        ResourceFile entity = new ResourceFile();
        String originName = file == null ? null : file.getOriginalFilename();
        entity.setOriginName(originName);
        String extension = file == null ? FileTypeUtils.getFileType(relativePath) : FileTypeUtils.getExtension(file);
        entity.setFileType(ResourceFileTypeEnum.resolveByExtension(extension));
        entity.setPath(accessUrl);
        entity.setRelativePath(relativePath);
        entity.setLength(length == null ? 0L : length);
        entity.setFileMd5(fileMd5);
        entity.setApplyUploadCount(1);
        entity.setChannelId(channelId);
        entity.setBucketName(bucketName);
        // gmtCreate / createdUserId 等由 MetaObjectHandler 填充
        resourceFileMapper.insert(entity);
        return entity;
    }

    @Override
    public void deleteByPath(String path) {
        if (StringUtils.isEmpty(path)) {
            return;
        }
        // 逻辑删除：按实体删除以便触发审计填充
        LambdaQueryWrapper<ResourceFile> wrapper = Wrappers.lambdaQuery(ResourceFile.class);
        wrapper.eq(ResourceFile::getPath, path).or().eq(ResourceFile::getRelativePath, path);
        resourceFileMapper.delete(wrapper);
    }

    @Override
    public int deleteByIds(Long[] ids) {
        if (ids == null || ids.length == 0) {
            return 0;
        }
        int rows = 0;
        for (Long id : ids) {
            if (id == null) {
                continue;
            }
            // 逐条删除以触发逻辑删除审计填充
            rows += resourceFileMapper.deleteById(id);
        }
        return rows;
    }
}
