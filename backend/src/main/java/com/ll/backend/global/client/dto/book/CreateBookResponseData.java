package com.ll.backend.global.client.dto.book;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CreateBookResponseData(
        String bookUid,
        BookCreateResponseBook book
) {}
