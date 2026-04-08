package com.ll.backend.global.client.dto.photo;

import com.ll.backend.global.client.dto.book.SweetbookApiEnvelope;

/** Sweetbook 업로드 후 로컬 저장 경로는 {@code originalRelativePath}(업로드 루트 기준 상대). */
public record PhotoUploadOutcome(
        SweetbookApiEnvelope<PhotoUploadData> envelope, String originalRelativePath) {}
