package com.ll.backend.domain.credit.dto;

public record CreditChargeApiResponse(boolean success, String message, CreditChargeDataDto data) {}

