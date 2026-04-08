package com.ll.backend.domain.sweetbook.support;

import com.ll.backend.global.client.dto.book.CreateBookResponseData;
import java.util.Map;
import java.util.Optional;

/** Sweetbook 책 생성 응답에서 {@code bookUid} 추출 (본문 형태가 바뀌어도 흔한 중첩을 허용) */
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

    public static Optional<String> extractBookUid(Map<String, Object> body) {
        if (body == null || body.isEmpty()) {
            return Optional.empty();
        }
        Optional<String> top = stringNonBlank(body.get("bookUid"));
        if (top.isPresent()) {
            return top;
        }
        Object data = body.get("data");
        if (!(data instanceof Map<?, ?> dataMap)) {
            return Optional.empty();
        }
        Optional<String> inData = stringNonBlank(dataMap.get("bookUid"));
        if (inData.isPresent()) {
            return inData;
        }
        Object book = dataMap.get("book");
        if (book instanceof Map<?, ?> bookMap) {
            return stringNonBlank(bookMap.get("bookUid"));
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
