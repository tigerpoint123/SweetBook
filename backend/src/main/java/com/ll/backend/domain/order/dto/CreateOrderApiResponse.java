package com.ll.backend.domain.order.dto;

public record CreateOrderApiResponse(boolean success, String message, CreateOrderDataDto data) {}
