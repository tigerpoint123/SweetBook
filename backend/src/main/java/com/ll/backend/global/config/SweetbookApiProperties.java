package com.ll.backend.global.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "sweetbook.api")
public record SweetbookApiProperties(
        @NotBlank String baseUrl,
        @NotBlank String key
) {
}
