package com.winter.cloud.common.filestorage.clients.huawei;

import com.winter.cloud.common.filestorage.AbstractLocationBucket;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 华为云分区
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2022/8/17 14:17
 */
@ToString(callSuper = true)
@Getter
@Setter
public class HuaWeiBucket extends AbstractLocationBucket {

    private static final long serialVersionUID = -5886505930238186252L;

    /**
     * @param name
     */
    public HuaWeiBucket(String name) {
        super(name);
    }
}
