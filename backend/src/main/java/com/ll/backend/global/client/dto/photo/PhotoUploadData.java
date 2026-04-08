package com.ll.backend.global.client.dto.photo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PhotoUploadData(
        String fileName,
        String originalName,
        long size,
        String mimeType,
        Instant uploadedAt,
        @JsonProperty("isDuplicate")
        boolean isDuplicate,
        String hash
) {}
