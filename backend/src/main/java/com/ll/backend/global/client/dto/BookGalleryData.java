package com.ll.backend.global.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BookGalleryData(
        List<BookPhotoItem> photos,
        int totalCount,
        boolean finalized
) {}
