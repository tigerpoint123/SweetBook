package com.ll.backend.global.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UploadedPhotoInfo(
        String fileName,
        String thumbnailUrl,
        int width,
        int height
) {}
