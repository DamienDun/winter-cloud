package com.winter.cloud.file.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Locale;

/**
 * 图片文字水印（对齐 paperless ImageWebpUtil；webp 依赖 org.sejda.imageio:webp-imageio）
 */
public final class ImageWatermarkUtil {

    private static final Logger log = LoggerFactory.getLogger(ImageWatermarkUtil.class);

    private ImageWatermarkUtil() {
    }

    /**
     * 给图片流添加日期文字水印，返回新的内存流（失败返回 null）
     */
    public static InputStream addWatermark(InputStream source, String watermarkText, String extensionName) {
        try {
            BufferedImage image = ImageIO.read(source);
            if (image == null) {
                log.warn("加水印失败：无法解码图片，extension={}（webp 需 webp-imageio 插件）", extensionName);
                return null;
            }
            BufferedImage canvas = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = canvas.createGraphics();
            try {
                graphics.setColor(Color.WHITE);
                graphics.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
                graphics.drawImage(image, 0, 0, null);
                drawText(watermarkText, image, graphics, canvas);
            } finally {
                graphics.dispose();
            }

            String format = normalizeFormat(extensionName);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            if (!ImageIO.write(canvas, format, baos) || baos.size() == 0) {
                log.warn("加水印：格式 {} 写出失败，尝试 jpg", format);
                baos.reset();
                if (!ImageIO.write(canvas, "jpg", baos) || baos.size() == 0) {
                    log.error("加水印失败：ImageIO 无法写出图片, writers={}",
                            String.join(",", ImageIO.getWriterFormatNames()));
                    return null;
                }
            }
            return new ByteArrayInputStream(baos.toByteArray());
        } catch (Exception e) {
            log.error("加水印失败", e);
            return null;
        }
    }

    private static void drawText(String watermarkText, BufferedImage image, Graphics2D graphics, BufferedImage canvas) {
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
        int fontSize = Math.max(image.getHeight() / 20, 12);
        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, fontSize));
        int x = 10;
        int y = canvas.getHeight() - 10;
        graphics.setColor(new Color(0, 0, 0, 160));
        graphics.drawString(watermarkText, x - 1, y);
        graphics.drawString(watermarkText, x + 1, y);
        graphics.drawString(watermarkText, x, y - 1);
        graphics.drawString(watermarkText, x, y + 1);
        graphics.setColor(new Color(255, 255, 255, 200));
        graphics.drawString(watermarkText, x, y);
    }

    /**
     * 保留 webp（需 webp-imageio）；jpeg→jpg；未知格式回退 jpg
     */
    private static String normalizeFormat(String extensionName) {
        if (extensionName == null || extensionName.isEmpty()) {
            return "jpg";
        }
        String ext = extensionName.toLowerCase(Locale.ROOT);
        if ("jpeg".equals(ext)) {
            return "jpg";
        }
        if ("png".equals(ext) || "jpg".equals(ext) || "gif".equals(ext)
                || "bmp".equals(ext) || "webp".equals(ext)) {
            return ext;
        }
        return "jpg";
    }
}
