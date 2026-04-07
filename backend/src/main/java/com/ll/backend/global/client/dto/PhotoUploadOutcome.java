package com.ll.backend.global.client.dto;

public record PhotoUploadOutcome(
        SweetbookApiEnvelope<PhotoUploadData> envelope,
        String originalLocalPath
) {}
