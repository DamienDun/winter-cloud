package com.winter.cloud.common.filestorage.clients.fastdfs;

import com.winter.cloud.common.filestorage.AbstractBucket;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * FastDFS 分区信息
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2022/8/17 14:00
 */
@ToString(callSuper = true)
@Getter
@Setter
public class FastDFSBucket extends AbstractBucket {

    private static final long serialVersionUID = -1799448666278756043L;

    /**
     * @param name
     */
    public FastDFSBucket(String name) {
        super(name);
    }
}
