package com.winter.cloud.common.core.constant;

/**
 * 通用常量信息
 *
 * @author winter
 */
public interface Constants {
    /**
     * UTF-8 字符集
     */
    public static final String UTF8 = "UTF-8";

    /**
     * GBK 字符集
     */
    public static final String GBK = "GBK";

    /**
     * www主域
     */
    public static final String WWW = "www.";

    /**
     * RMI 远程方法调用
     */
    public static final String LOOKUP_RMI = "rmi:";

    /**
     * LDAP 远程方法调用
     */
    public static final String LOOKUP_LDAP = "ldap:";

    /**
     * LDAPS 远程方法调用
     */
    public static final String LOOKUP_LDAPS = "ldaps:";

    /**
     * http请求
     */
    public static final String HTTP = "http://";

    /**
     * https请求
     */
    public static final String HTTPS = "https://";

    /**
     * 成功标记
     */
    public static final Integer SUCCESS = 200;

    /**
     * 失败标记
     */
    public static final Integer FAIL = 500;

    /**
     * 登录成功状态
     */
    public static final String LOGIN_SUCCESS_STATUS = "0";

    /**
     * 登录失败状态
     */
    public static final String LOGIN_FAIL_STATUS = "1";

    /**
     * 登录成功
     */
    public static final String LOGIN_SUCCESS = "Success";

    /**
     * 注销
     */
    public static final String LOGOUT = "Logout";

    /**
     * 注册
     */
    public static final String REGISTER = "Register";

    /**
     * 登录失败
     */
    public static final String LOGIN_FAIL = "Error";

    /**
     * 所有权限标识
     */
    public static final String ALL_PERMISSION = "*:*:*";

    /**
     * 管理员角色权限标识
     */
    public static final String SUPER_ADMIN = "admin";

    /**
     * 当前记录起始索引
     */
    public static final String PAGE_NUM = "pageNum";

    /**
     * 每页显示记录数
     */
    public static final String PAGE_SIZE = "pageSize";

    /**
     * 排序列
     */
    public static final String ORDER_BY_COLUMN = "orderByColumn";

    /**
     * 排序的方向 "desc" 或者 "asc".
     */
    public static final String IS_ASC = "isAsc";

    /**
     * 验证码有效期（分钟）
     */
    public static final long CAPTCHA_EXPIRATION = 2;

    /**
     * 资源映射路径 前缀
     */
    public static final String RESOURCE_PREFIX = "/profile";

    /**
     * 自动识别json对象白名单配置（仅允许解析的包名，范围越小越安全）
     * <p>必须包含本项目包名，否则 Redis 中带 @type 的 LoginUser 等无法反序列化。</p>
     */
    public static final String[] JSON_WHITELIST_STR = {"com.winter.cloud"};

    /**
     * 定时任务白名单配置（仅允许访问的包名，如其他需要可以自行添加）
     */
    public static final String[] JOB_WHITELIST_STR = {"com.winter.cloud.job.task"};

    /**
     * 定时任务违规的字符
     */
    public static final String[] JOB_ERROR_STR = {"java.net.URL", "javax.naming.InitialContext", "org.yaml.snakeyaml",
            "org.springframework", "org.apache", "com.winter.cloud.common.core.utils.file"};

    /**
     * JWT方式生成token
     */
    String TOKEN_CREATE_JWT = "jwt";

    /**
     * redis普通方式生成token(实际也是jwt,只是用了redis去缓存)
     */
    String TOKEN_CREATE_REDIS = "redis";

    /**
     * 响应头业务码
     */
    String RESP_HEADER_CODE = "resp-code";

    /**
     * 响应头时间
     */
    String RESP_HEADER_DATE = "resp-date";

    /**
     * CORS 暴露自定义响应头
     */
    String RESP_ACCESS_CONTROL_EXPOSE_HEADERS = "Access-Control-Expose-Headers";

    /**
     * 逗号切割正则（兼容中英文逗号）
     */
    String COMMA_SPLIT_REGEX = ",|，";

    /**
     * 冒号
     */
    String COLON = ":";

    /**
     * 点号
     */
    String DOT = ".";

    /**
     * 英文逗号
     */
    String ENGLISH_COMMA = ",";
}
