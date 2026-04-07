package com.ll.backend.global.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BooksListData(
        List<BookListItem> books,
        Integer total,
        Integer limit,
        Integer offset
) {}
