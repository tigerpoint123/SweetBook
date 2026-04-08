package com.ll.backend.global.client.dto.credit;

public record CreditChargeRequestPayload(
        long amount,
        String memo
) {
}
