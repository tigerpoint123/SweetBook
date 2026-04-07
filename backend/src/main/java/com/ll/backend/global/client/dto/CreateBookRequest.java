package com.ll.backend.global.client.dto;

public record CreateBookRequest(
        String title,
         String bookSpecUid,
         String bookAuthor,
         String externalRef
) {
}
