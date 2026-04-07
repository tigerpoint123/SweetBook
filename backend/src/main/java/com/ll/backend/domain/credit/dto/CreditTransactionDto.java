package com.ll.backend.domain.credit.dto;

import com.ll.backend.global.client.dto.CreditTransactionItem;

public record CreditTransactionDto(
        String transactionId,
        String accountUid,
        Integer reasonCode,
        String reasonDisplay,
        String direction,
        Long amount,
        Long balanceAfter,
        String memo,
        Boolean isTest,
        String createdAt) {
    public static CreditTransactionDto from(CreditTransactionItem row) {
        return new CreditTransactionDto(
                row.transactionId(),
                row.accountUid(),
                row.reasonCode(),
                row.reasonDisplay(),
                row.direction(),
                row.amount(),
                row.balanceAfter(),
                row.memo(),
                row.isTest(),
                row.createdAt());
    }
}
