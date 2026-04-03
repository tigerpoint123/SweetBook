package com.ll.backend.global.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SweetbookApiEnvelope<T>(
        boolean success,
        T data
) {}
