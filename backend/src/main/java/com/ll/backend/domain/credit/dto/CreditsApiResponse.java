package com.ll.backend.domain.credit.dto;

import com.ll.backend.global.client.dto.book.SweetbookApiResponse;
import com.ll.backend.global.client.dto.credit.CreditBalanceData;

public record CreditsApiResponse(boolean success, String message, CreditsDataDto data) {
    public static CreditsApiResponse from(SweetbookApiResponse<CreditBalanceData> response) {
        CreditBalanceData data = CreditBalanceData.fromNullable(response.data());
        return new CreditsApiResponse(response.success(), response.message(), data.toDto());
    }
}
