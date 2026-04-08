package com.ll.backend.domain.sweetbook.support;

import com.ll.backend.global.client.dto.book.CreateBookResponseData;

import java.util.Optional;

public final class SweetbookCreateResponseParser {

    private SweetbookCreateResponseParser() {}

    public static Optional<String> extractBookUid(CreateBookResponseData data) {
        if (data == null) {
            return Optional.empty();
        }
        Optional<String> top = stringNonBlank(data.bookUid());
        if (top.isPresent()) {
            return top;
        }
        if (data.book() != null) {
            return stringNonBlank(data.book().bookUid());
        }
        return Optional.empty();
    }

    private static Optional<String> stringNonBlank(Object v) {
        if (v instanceof String s) {
            String t = s.trim();
            if (!t.isEmpty()) {
                return Optional.of(t);
            }
        }
        return Optional.empty();
    }
}
