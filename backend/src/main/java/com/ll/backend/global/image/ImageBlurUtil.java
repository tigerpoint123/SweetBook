package com.ll.backend.global.image;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;

/** Thumbnailator + 5×5·7×7 {@link BoxBlurImageFilter} 순차 적용으로 블러 복제본 생성. 실패 시 원본 바이트 복사로 대체합니다. */
@Slf4j
public final class ImageBlurUtil {

    private ImageBlurUtil() {}

    public static void blurToFileOrCopy(Path source, Path dest) {
        try {
            String destName = dest.getFileName().toString();
            int dot = destName.lastIndexOf('.');
            String fmt = formatName(destName, dot);
            Thumbnails.of(source.toFile())
                    .scale(1.0)
                    .addFilter(new BoxBlurImageFilter(5))
                    .addFilter(new BoxBlurImageFilter(7))
                    .outputFormat(fmt)
                    .toFile(dest.toFile());
        } catch (Exception e) {
            log.warn("Thumbnailator 블러 실패, 원본 복사로 대체 source={} msg={}", source, e.getMessage());
            try {
                Files.createDirectories(dest.getParent());
                Files.copy(source, dest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        }
    }

    private static String formatName(String destName, int dot) {
        if (dot <= 0 || dot >= destName.length() - 1) {
            return "png";
        }
        String ext = destName.substring(dot + 1).toLowerCase();
        if (ext.equals("jpg") || ext.equals("jpeg")) {
            return "jpeg";
        }
        if (ext.equals("png")) {
            return "png";
        }
        if (ext.equals("gif")) {
            return "gif";
        }
        return "png";
    }
}
