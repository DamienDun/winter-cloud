package com.winter.cloud.common.filestorage;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Date;

/**
 * 位置分区
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2022/8/16 15:42
 */
@ToString(callSuper = true)
@Getter
@Setter
public class AbstractLocationBucket extends AbstractBucket {

    private static final long serialVersionUID = 9083164496327686716L;

    /**
     * @param name
     */
    public AbstractLocationBucket(String name) {
        super(name);
    }

    /**
     * 位置
     */
    private String location;

    /**
     * 创建时间
     */
    private Date creationDate;
}
