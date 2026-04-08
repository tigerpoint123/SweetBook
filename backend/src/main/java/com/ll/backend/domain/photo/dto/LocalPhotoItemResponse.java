package com.ll.backend.domain.photo.dto;

import java.time.Instant;

public record LocalPhotoItemResponse(
        Long id,
        String bookUid,
        String originalName,
        String sweetbookFileName,
        Long size,
        String mimeType,
        Instant uploadedAt,
        String hash,
        boolean isDuplicate,
        String fileUrl,
        Long price
) {}
