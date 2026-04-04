package com.ll.backend.domain.photo.dto;

import java.time.Instant;

public record BookCoverItemResponse(
        String bookUid, Long photoId, String fileUrl, String subtitle, String dateRange, Instant updatedAt) {}
