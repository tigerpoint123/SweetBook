package com.ll.backend.global.client.dto.book;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AddBookContentsResponse(
        boolean success,
        String message,
        Map<String, Object> data
) {
}
