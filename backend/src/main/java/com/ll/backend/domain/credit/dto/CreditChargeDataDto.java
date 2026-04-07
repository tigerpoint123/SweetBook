package com.ll.backend.domain.credit.dto;

public record CreditChargeDataDto(
        String transactionUid,
        Long amount,
        Long balanceAfter,
        String currency) {}

