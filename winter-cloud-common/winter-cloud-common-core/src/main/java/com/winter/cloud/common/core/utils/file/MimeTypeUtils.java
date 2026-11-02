package com.winter.cloud.common.core.utils.file;

/**
 * 媒体类型工具类
 *
 * @author ruoyi
 */
public class MimeTypeUtils
{
    public static final String IMAGE_PNG = "image/png";

    public static final String IMAGE_JPG = "image/jpg";

    public static final String IMAGE_JPEG = "image/jpeg";

    public static final String IMAGE_BMP = "image/bmp";

    public static final String IMAGE_GIF = "image/gif";

    public static final String[] IMAGE_EXTENSION = {
            "bmp", "gif", "jpg", "jpeg", "png", "webp", "svg", "tif", "tiff", "ico", "heic", "heif", "apng", "dib"
    };

    public static final String[] FLASH_EXTENSION = { "swf", "flv" };

    public static final String[] MEDIA_EXTENSION = {
            "swf", "flv", "mp3", "wav", "wma", "wmv", "mid", "midi", "avi", "mpg", "mpeg",
            "asf", "rm", "rmvb", "aac", "flac", "ogg", "m4a", "opus"
    };

    public static final String[] VIDEO_EXTENSION = {
            "mp4", "avi", "rmvb", "mkv", "mov", "wmv", "flv", "webm", "m4v", "mpeg", "mpg", "3gp", "ts"
    };

    /**
     * 通用上传允许后缀（不含 exe/bat/sh/jsp 等可执行与服务端脚本）
     */
    public static final String[] DEFAULT_ALLOWED_EXTENSION = {
            // 图片
            "bmp", "gif", "jpg", "jpeg", "png", "webp", "svg", "tif", "tiff", "ico", "heic", "heif", "apng", "dib",
            // 办公文档
            "doc", "docx", "xls", "xlsx", "xlsm", "ppt", "pptx", "pdf", "rtf", "odt", "ods", "odp",
            "wps", "et", "dps", "ofd",
            // 文本 / 网页 / 标记
            "txt", "md", "markdown", "csv", "html", "htm", "xml", "json", "yml", "yaml", "log", "ini", "conf", "properties", "sql",
            // 压缩包
            "rar", "zip", "7z", "gz", "bz2", "tar", "xz", "tgz",
            // 音频
            "mp3", "wav", "flac", "aac", "ogg", "m4a", "wma", "mid", "midi", "opus", "amr",
            // 视频
            "mp4", "avi", "rmvb", "mkv", "mov", "wmv", "flv", "webm", "m4v", "mpeg", "mpg", "3gp", "ts",
            // 设计 / 常用其它
            "psd", "ai", "eps", "sketch", "fig"
    };

    public static String getExtension(String prefix)
    {
        switch (prefix)
        {
            case IMAGE_PNG:
                return "png";
            case IMAGE_JPG:
                return "jpg";
            case IMAGE_JPEG:
                return "jpeg";
            case IMAGE_BMP:
                return "bmp";
            case IMAGE_GIF:
                return "gif";
            default:
                return "";
        }
    }
}
