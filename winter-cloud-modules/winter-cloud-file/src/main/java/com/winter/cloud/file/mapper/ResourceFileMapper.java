package com.winter.cloud.file.mapper;

import com.winter.cloud.common.mybatis.mapper.DefaultBaseMapper;
import com.winter.cloud.file.domain.entity.ResourceFile;
import org.apache.ibatis.annotations.Mapper;

/**
 * 资源文件 Mapper
 */
@Mapper
public interface ResourceFileMapper extends DefaultBaseMapper<ResourceFile> {
}
