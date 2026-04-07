package com.ll.backend.domain.order.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderSummaryItemDto(
        String orderUid,
        int orderStatus,
        String orderStatusDisplay,
        BigDecimal totalAmount,
        Instant orderedAt) {}
