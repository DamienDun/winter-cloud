package com.winter.cloud.xxljob.mapper;

import com.winter.cloud.xxljob.domain.XxlJobLogGlue;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * job log for glue
 * @author xuxueli 2016-5-19 18:04:56
 */
@Mapper
public interface XxlJobLogGlueMapper {
	
	public int save(XxlJobLogGlue xxlJobLogGlue);
	
	public List<XxlJobLogGlue> findByJobId(@Param("jobId") int jobId);

	public int removeOld(@Param("jobId") int jobId, @Param("limit") int limit);

	public int deleteByJobId(@Param("jobId") int jobId);

    /**
     * 根据任务id批量删除
     *
     * @param jobIds
     * @return
     */
    int batchDeleteByJobId(@Param("jobIds") List<Long> jobIds);
}
