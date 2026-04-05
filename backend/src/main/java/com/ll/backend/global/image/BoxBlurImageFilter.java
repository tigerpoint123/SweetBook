package com.ll.backend.global.image;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.util.Arrays;
import net.coobird.thumbnailator.filters.ImageFilter;

public final class BoxBlurImageFilter implements ImageFilter {

    private final int size;
    private final float[] kernelData;

    public BoxBlurImageFilter(int size) {
        if (size < 3 || (size & 1) == 0) {
            throw new IllegalArgumentException("size must be odd and >= 3");
        }
        this.size = size;
        int cells = size * size;
        float v = 1f / cells;
        this.kernelData = new float[cells];
        Arrays.fill(this.kernelData, v);
    }

    @Override
    public BufferedImage apply(BufferedImage img) {
        BufferedImage rgb =
                img.getType() == BufferedImage.TYPE_INT_RGB
                        ? img
                        : new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
        if (img != rgb) {
            Graphics2D g = rgb.createGraphics();
            try {
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.drawImage(img, 0, 0, null);
            } finally {
                g.dispose();
            }
        }
        ConvolveOp op = new ConvolveOp(new Kernel(size, size, kernelData), ConvolveOp.EDGE_NO_OP, null);
        return op.filter(rgb, null);
    }
}
