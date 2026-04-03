package com.ll.backend.global.client.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateBookRequest(
        String title,
         String bookSpecUid,
         String bookAuthor,
         String externalRef
) {
}
