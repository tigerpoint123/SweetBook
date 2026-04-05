package com.ll.backend.global.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/** GET …/gallery 응답 — Sweetbook 사진 목록 + 로컬 DB 최종화 여부 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BookGalleryData(
        List<BookPhotoItem> photos,
        int totalCount,
        boolean finalized
) {}
