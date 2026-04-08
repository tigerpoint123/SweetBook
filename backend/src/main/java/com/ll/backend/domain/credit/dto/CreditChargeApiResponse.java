package com.ll.backend.domain.credit.dto;

import com.ll.backend.global.client.dto.book.SweetbookApiResponse;
import com.ll.backend.global.client.dto.credit.CreditChargeData;

public record CreditChargeApiResponse(boolean success, String message, CreditChargeDataDto data) {
    public static CreditChargeApiResponse from(SweetbookApiResponse<CreditChargeData> response) {
        CreditChargeData d = response.data();
        return new CreditChargeApiResponse(
                response.success(),
                response.message(),
                new CreditChargeDataDto(
                        d.transactionUid(),
                        d.amount(),
                        d.balanceAfter(),
                        d.currency()));
    }
}

