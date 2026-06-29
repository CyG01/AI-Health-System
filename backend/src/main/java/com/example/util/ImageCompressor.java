package com.example.util;

import com.example.common.BusinessException;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Set;

/**
 * 图片压缩工具类 —— 防止大图 OOM
 *
 * 处理流程：
 * 1. 校验文件大小（≤10MB）
 * 2. 按比例缩放至最大 1024x1024
 * 3. 以 JPEG 格式输出（quality 0.7）
 */
public class ImageCompressor {

    private static final int MAX_WIDTH = 1024;
    private static final int MAX_HEIGHT = 1024;
    private static final float JPEG_QUALITY = 0.7f;
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final Set<String> ALLOWED_FORMATS = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp"
    );

    /**
     * 压缩图片并返回字节数组
     */
    public static byte[] compress(MultipartFile file) throws IOException {
        // 文件大小校验
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException("图片大小不能超过10MB");
        }
        // 格式校验
        String contentType = file.getContentType();
        if (contentType != null && !ALLOWED_FORMATS.contains(contentType.toLowerCase())) {
            throw new BusinessException("不支持的图片格式，仅支持 JPG/PNG/WebP");
        }

        BufferedImage original = ImageIO.read(file.getInputStream());
        if (original == null) {
            throw new BusinessException("无法解析图片文件，请确认文件格式正确");
        }

        // 按比例缩放至最大 1024x1024
        int w = original.getWidth();
        int h = original.getHeight();
        if (w > MAX_WIDTH || h > MAX_HEIGHT) {
            double ratio = Math.min((double) MAX_WIDTH / w, (double) MAX_HEIGHT / h);
            w = (int) (w * ratio);
            h = (int) (h * ratio);

            BufferedImage scaled = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = scaled.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(original, 0, 0, w, h, null);
            g.dispose();
            original = scaled;
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // 使用 JPEGImageWriteParam 控制压缩质量
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(out)) {
            writer.setOutput(ios);
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(JPEG_QUALITY);
            writer.write(null, new IIOImage(original, null, null), param);
        } finally {
            writer.dispose();
        }
        return out.toByteArray();
    }

    /**
     * 压缩并转为 Base64（一步到位）
     */
    public static String compressToBase64(MultipartFile file) throws IOException {
        byte[] compressed = compress(file);
        return java.util.Base64.getEncoder().encodeToString(compressed);
    }
}