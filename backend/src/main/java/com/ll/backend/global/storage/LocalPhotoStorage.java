package com.ll.backend.global.storage;

import com.ll.backend.global.dto.SavedPaths;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class LocalPhotoStorage {

    public static final String SUBDIR_ORIGINAL = "original";

    @Value("${app.photo.upload-dir:uploads/photos}")
    private String uploadDir;

    public SavedPaths save(String bookUid, byte[] bytes, String originalFilename) {
        String safe = sanitizeFilename(originalFilename);
        String name = System.currentTimeMillis() + "_" + safe;
        Path rootBook = Paths.get(uploadDir).resolve(bookUid).normalize();
        Path originalDir = rootBook.resolve(SUBDIR_ORIGINAL);
        try {
            Files.createDirectories(originalDir);
            Path originalFile = originalDir.resolve(name);
            Files.write(originalFile, bytes);
            Path uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path abs = originalFile.toAbsolutePath().normalize();
            String rel = uploadRoot.relativize(abs).toString().replace('\\', '/');
            log.info("로컬 사진 저장 absolute={} relative={}", abs, rel);
            return new SavedPaths(rel);
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
        Path root = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path path = resolveStoredPath(storedPath, root);
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

    /** DB에 절대 경로로 남아 있는 기존 행과 상대 경로 행 모두 지원. */
    public static Path resolveStoredPath(String storedPath, Path uploadRootAbsolute) {
        Path p = Paths.get(storedPath);
        if (p.isAbsolute()) {
            return p.normalize();
        }
        return uploadRootAbsolute.resolve(storedPath).normalize().toAbsolutePath();
    }
}
