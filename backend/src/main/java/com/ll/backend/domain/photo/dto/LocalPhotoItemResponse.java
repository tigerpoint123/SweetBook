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
        /** 세션·최종화·샘플 여부에 따라 서버가 원본 또는 블러를 내주는 URL */
        String fileUrl,
        /** 콘텐츠 추가 시 선정된 공개 샘플 여부 */
        boolean isSample,
        /** 원본 바이너리 (권한 있을 때만 200) */
        String originalUrl,
        /** 블러 바이너리 (항상 블러 파일) */
        String blurUrl) {}
