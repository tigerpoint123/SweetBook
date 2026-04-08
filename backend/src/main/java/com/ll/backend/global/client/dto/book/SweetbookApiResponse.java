package com.ll.backend.global.client.dto.book;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SweetbookApiResponse<T>(
        boolean success,
        String message,
        T data
) {

}
