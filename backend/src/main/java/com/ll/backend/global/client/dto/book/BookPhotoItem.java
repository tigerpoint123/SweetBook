package com.ll.backend.global.client.dto.book;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BookPhotoItem(
        String fileName,
        String originalName,
        long size,
        String mimeType,
        Instant uploadedAt,
        String hash
) {}
