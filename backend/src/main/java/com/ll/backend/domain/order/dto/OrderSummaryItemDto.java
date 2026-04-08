package com.ll.backend.domain.order.dto;

import java.time.Instant;

public record OrderSummaryItemDto(
        String orderUid,
        int orderStatus,
        String orderStatusDisplay,
        long totalAmount,
        Instant orderedAt) {}
