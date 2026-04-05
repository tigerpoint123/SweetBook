package com.ll.backend.global.image;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import javax.imageio.ImageIO;
import lombok.extern.slf4j.Slf4j;

/** Thumbnailator가 가우시안 필터를 제공하지 않아, JDK {@link ConvolveOp}로 블러 이미지를 만듭니다. */
@Slf4j
public final class ImageBlurWriter {

    private static final int BOX_RADIUS = 6;
    private static final int PASS_COUNT = 2;

    private ImageBlurWriter() {}

    public static void writeBlurredCopy(Path sourceFile, Path destFile) throws IOException {
        BufferedImage src = ImageIO.read(sourceFile.toFile());
        if (src == null) {
            throw new IOException("이미지로 읽을 수 없습니다: " + sourceFile);
        }
        BufferedImage rgb =
                src.getType() == BufferedImage.TYPE_INT_RGB
                        ? src
                        : convertToRgb(src);
        BufferedImage cur = rgb;
        for (int i = 0; i < PASS_COUNT; i++) {
            cur = boxBlurOnce(cur);
        }
        String name = destFile.getFileName().toString().toLowerCase();
        String format = name.endsWith(".png") ? "png" : "jpg";
        if ("jpg".equals(format) || "jpeg".equals(format)) {
            BufferedImage out = new BufferedImage(cur.getWidth(), cur.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = out.createGraphics();
            g.drawImage(cur, 0, 0, null);
            g.dispose();
            cur = out;
        }
        Files.createDirectories(destFile.getParent());
        if (!ImageIO.write(cur, format, destFile.toFile())) {
            throw new IOException("블러 이미지 저장 실패: " + destFile);
        }
    }

    private static BufferedImage convertToRgb(BufferedImage src) {
        BufferedImage rgb = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgb.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return rgb;
    }

    private static BufferedImage boxBlurOnce(BufferedImage src) {
        int n = 2 * BOX_RADIUS + 1;
        float w = 1f / (n * n);
        float[] data = new float[n * n];
        Arrays.fill(data, w);
        ConvolveOp op = new ConvolveOp(new Kernel(n, n, data), ConvolveOp.EDGE_NO_OP, null);
        return op.filter(src, null);
    }
}
