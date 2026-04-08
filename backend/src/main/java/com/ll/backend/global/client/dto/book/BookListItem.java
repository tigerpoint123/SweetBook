package com.ll.backend.global.client.dto.book;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BookListItem(
        String bookUid,
        String title,
        String bookSpecUid,
        String status,
        String pdfStatus,
        Instant pdfRequestedAt,
        Instant createdAt,
        String externalRef
) {}
