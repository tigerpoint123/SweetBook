package com.ll.backend.domain.order.dto;

public record OrdersListApiResponse(boolean success, String message, OrdersListDataDto data) {}
