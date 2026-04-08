package com.ll.backend.global.client.dto.photo;

import com.ll.backend.global.client.dto.book.SweetbookApiEnvelope;

public record PhotoUploadOutcome(
        SweetbookApiEnvelope<PhotoUploadData> envelope,
        String originalLocalPath
) {}
