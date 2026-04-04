package com.ll.backend.domain.sweetbook.vo;

import com.ll.backend.domain.sweetbook.entity.SweetbookBook;

public record MyBookItemResponse(String bookUid, String createdAt) {

    public static MyBookItemResponse from(SweetbookBook entity) {
        return new MyBookItemResponse(entity.getBookUid(), entity.getCreatedAt().toString());
    }
}
