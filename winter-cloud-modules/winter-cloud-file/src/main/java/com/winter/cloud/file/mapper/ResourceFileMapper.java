package com.winter.cloud.file.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.winter.cloud.file.domain.ResourceFile;
import org.apache.ibatis.annotations.Mapper;

/**
 * 资源文件 Mapper
 */
@Mapper
public interface ResourceFileMapper extends BaseMapper<ResourceFile> {
}