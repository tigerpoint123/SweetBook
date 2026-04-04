package com.ll.backend.domain.sweetbook.support;

/**
 * Sweetbook POST /v1/books/{bookUid}/contents (multipart) 기본값.
 * {@code application.yml}의 {@code sweetbook.contents.*}로 덮어쓸 수 있습니다.
 */
public final class SweetbookContentDefaults {

    public static final String TEMPLATE_UID = "1vuzMfUnCkXS";

    public static final String BREAK_BEFORE = "page";

    private SweetbookContentDefaults() {}
}
