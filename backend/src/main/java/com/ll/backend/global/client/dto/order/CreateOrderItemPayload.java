package com.ll.backend.global.client.dto.order;

public record CreateOrderItemPayload(
        String bookUid,
        int quantity
) {
}
