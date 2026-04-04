package com.ll.backend.global.storage;

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

    @Value("${app.photo.upload-dir:uploads/photos}")
    private String uploadDir;

    public String save(String bookUid, byte[] bytes, String originalFilename) {
        String safe = sanitizeFilename(originalFilename);
        String name = System.currentTimeMillis() + "_" + safe;
        Path dir = Paths.get(uploadDir).resolve(bookUid).normalize();
        try {
            Files.createDirectories(dir);
            Path file = dir.resolve(name);
            Files.write(file, bytes);
            String path = file.toAbsolutePath().toString().replace('\\', '/');
            log.info("로컬 사진 저장 path={}", path);
            return path;
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
}
