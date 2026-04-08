package com.ll.backend.global.client.dto.credit;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CreditTransactionItem(
        String transactionId,
        String accountUid,
        Integer reasonCode,
        String reasonDisplay,
        String direction,
        Long amount,
        Long balanceAfter,
        String memo,
        Boolean isTest,
        String createdAt
) {
    public CreditTransactionItem {
        transactionId = normalizeText(transactionId);
        accountUid = normalizeText(accountUid);
        reasonDisplay = normalizeText(reasonDisplay);
        direction = normalizeText(direction);
        memo = normalizeText(memo);
        createdAt = normalizeText(createdAt);
    }

    private static String normalizeText(String value) {
        return value != null && !value.isBlank() ? value.trim() : null;
    }
}
