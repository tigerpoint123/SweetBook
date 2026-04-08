package com.ll.backend.global.client.dto.credit;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CreditChargeData(
        String transactionUid,
        Long amount,
        Long balanceAfter,
        String currency
) {
}
