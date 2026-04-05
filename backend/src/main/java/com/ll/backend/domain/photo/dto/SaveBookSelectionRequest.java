package com.ll.backend.domain.photo.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

/** POST …/selected 본문 — 이번 채택에서 추가할 photo id 목록(기존 행 유지·뒤에 추가) */
public record SaveBookSelectionRequest(@NotNull List<Long> photoIds) {

    public SaveBookSelectionRequest {
        photoIds = photoIds != null ? List.copyOf(photoIds) : List.of();
    }
}
