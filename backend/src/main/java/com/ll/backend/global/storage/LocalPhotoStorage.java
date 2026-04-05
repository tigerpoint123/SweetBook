package com.ll.backend.global.storage;

import com.ll.backend.global.image.ImageBlurUtil;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class LocalPhotoStorage {

    public static final String SUBDIR_ORIGINAL = "original";
    public static final String SUBDIR_BLUR = "blur";

    @Value("${app.photo.upload-dir:uploads/photos}")
    private String uploadDir;

    public record SavedPaths(String originalAbsolutePath, String blurAbsolutePath) {}

    /**
     * {@code uploads/photos/{bookUid}/original/} 에 원본 저장 후, 동일 파일명으로 {@code .../blur/} 에 블러 복제본을 만듭니다.
     */
    public SavedPaths save(String bookUid, byte[] bytes, String originalFilename) {
        String safe = sanitizeFilename(originalFilename);
        String name = System.currentTimeMillis() + "_" + safe;
        Path rootBook = Paths.get(uploadDir).resolve(bookUid).normalize();
        Path originalDir = rootBook.resolve(SUBDIR_ORIGINAL);
        Path blurDir = rootBook.resolve(SUBDIR_BLUR);
        try {
            Files.createDirectories(originalDir);
            Files.createDirectories(blurDir);
            Path originalFile = originalDir.resolve(name);
            Files.write(originalFile, bytes);
            Path blurFile = blurDir.resolve(name);
            ImageBlurUtil.blurToFileOrCopy(originalFile, blurFile);
            String op = originalFile.toAbsolutePath().toString().replace('\\', '/');
            String bp = blurFile.toAbsolutePath().toString().replace('\\', '/');
            log.info("로컬 사진 저장 original={} blur={}", op, bp);
            return new SavedPaths(op, bp);
        } catch (IOException e) {
            throw new UncheckedIOException("로컬 사진 저장 실패 bookUid=" + bookUid, e);
        }
    }

    private static String sanitizeFilename(String name) {
        if (name == null || name.isBlank()) {
            return "photo.bin";
        }
        String base = name.replaceAll("[\\\\/:*?\"<>|]", "_");
        return base.length() > 200 ? base.substring(0, 200) : base;
    }

    public void deleteIfUnderUploadRoot(String storedPath) {
        if (storedPath == null || storedPath.isBlank()) {
            return;
        }
        Path path = Paths.get(storedPath).normalize().toAbsolutePath();
        Path root = Paths.get(uploadDir).toAbsolutePath().normalize();
        if (!path.startsWith(root) || !Files.isRegularFile(path)) {
            log.warn("로컬 사진 삭제 스킵(경로 불일치 또는 없음) path={}", storedPath);
            return;
        }
        try {
            Files.deleteIfExists(path);
            log.info("로컬 사진 파일 삭제 path={}", path);
        } catch (IOException e) {
            throw new UncheckedIOException("로컬 사진 파일 삭제 실패 path=" + storedPath, e);
        }
    }

    public String getUploadDir() {
        return uploadDir;
    }
}
