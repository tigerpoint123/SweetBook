package com.ll.backend.domain.order.dto;

import java.util.List;

public record OrdersListDataDto(
        long total, int limit, int offset, boolean hasNext, List<OrderSummaryItemDto> items) {}
