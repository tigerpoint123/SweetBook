package com.ll.backend.domain.credit.dto;

public record CreditTransactionsApiResponse(
        boolean success, String message, CreditTransactionsDataDto data) {}
