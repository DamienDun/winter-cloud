package com.winter.cloud.file.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.winter.cloud.common.core.constant.Constants;
import com.winter.cloud.common.core.context.SecurityContextHolder;
import com.winter.cloud.common.core.enums.CommonEnum;
import com.winter.cloud.common.core.utils.DateUtils;
import com.winter.cloud.common.core.utils.StringUtils;
import com.winter.cloud.common.core.utils.file.FileTypeUtils;
import com.winter.cloud.file.domain.entity.ResourceFile;
import com.winter.cloud.file.enums.ResourceFileTypeEnum;
import com.winter.cloud.file.mapper.ResourceFileMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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
                                         String channelId, String bucketName, String fileMd5, Long length,
                                         String module) {
        if (StringUtils.isNotEmpty(fileMd5)) {
            ResourceFile exist = queryByFileMd5(fileMd5);
            if (exist != null) {
                Integer count = exist.getApplyUploadCount() == null ? 1 : exist.getApplyUploadCount();
                exist.setApplyUploadCount(count + 1);
                if (file != null && StringUtils.isNotEmpty(file.getOriginalFilename())) {
                    exist.setOriginName(file.getOriginalFilename());
                }
                exist.setModule(mergeModule(exist.getModule(), module));
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
        entity.setModule(module);
        // gmtCreate / createdUserId 等由 MetaObjectHandler 填充
        resourceFileMapper.insert(entity);
        return entity;
    }

    private String mergeModule(String existModule, String newModule) {
        Set<String> moduleSet = new LinkedHashSet<>();
        if (StringUtils.isNotEmpty(existModule)) {
            moduleSet.addAll(Arrays.asList(existModule.split(Constants.ENGLISH_COMMA)));
        }
        if (StringUtils.isNotEmpty(newModule)) {
            moduleSet.add(newModule);
        }
        return moduleSet.isEmpty() ? existModule : String.join(Constants.ENGLISH_COMMA, moduleSet);
    }

    @Override
    public void deleteByPath(String path) {
        if (StringUtils.isEmpty(path)) {
            return;
        }
        LambdaQueryWrapper<ResourceFile> wrapper = Wrappers.lambdaQuery(ResourceFile.class);
        wrapper.eq(ResourceFile::getPath, path).or().eq(ResourceFile::getRelativePath, path);
        List<ResourceFile> list = resourceFileMapper.selectList(wrapper);
        for (ResourceFile item : list) {
            logicDelete(item.getId());
        }
    }

    @Override
    public int deleteByIds(Long[] ids) {
        if (ids == null || ids.length == 0) {
            return 0;
        }
        int rows = 0;
        for (Long id : ids) {
            rows += logicDelete(id);
        }
        return rows;
    }

    /**
     * 逻辑删除并显式填充删除时间/删除人。
     * 说明：deleteById(Long) 生成的逻辑删除 SQL 不会带 FieldFill.UPDATE 字段，
     * 因此这里用 UpdateWrapper 直接写 is_deleted / gmt_delete / deleted_user_id。
     */
    private int logicDelete(Long id) {
        if (id == null) {
            return 0;
        }
        Date now = DateUtils.getNowDate();
        Long userId = SecurityContextHolder.getUserId();
        LambdaUpdateWrapper<ResourceFile> uw = Wrappers.lambdaUpdate();
        uw.eq(ResourceFile::getId, id)
            .eq(ResourceFile::getDeleted, CommonEnum.Deleted.FALSE.getCode())
            .set(ResourceFile::getDeleted, CommonEnum.Deleted.TRUE.getCode())
            .set(ResourceFile::getGmtDelete, now)
            .set(ResourceFile::getGmtModified, now);
        if (userId != null) {
            uw.set(ResourceFile::getDeletedUserId, userId)
                .set(ResourceFile::getModifiedUserId, userId);
        }
        return resourceFileMapper.update(null, uw);
    }
}
