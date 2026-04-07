package com.ll.backend.domain.sweetbook.vo;

import com.ll.backend.domain.sweetbook.entity.SweetbookBook;

public record MyBookItemResponse(
        String bookUid, String createdAt, boolean finalized, Long price) {

    public static MyBookItemResponse from(SweetbookBook entity) {
        return new MyBookItemResponse(
                entity.getBookUid(),
                entity.getCreatedAt().toString(),
                entity.getFinalizedAt() != null,
                entity.getPrice());
    }
}
