package com.ll.backend.domain.credit.dto;

import com.ll.backend.global.client.dto.book.SweetbookApiResponse;
import com.ll.backend.global.client.dto.credit.CreditTransactionsData;
import java.util.List;

public record CreditTransactionsApiResponse(
        boolean success, String message, CreditTransactionsDataDto data) {
    public static CreditTransactionsApiResponse from(
            SweetbookApiResponse<CreditTransactionsData> response) {
        CreditTransactionsData d = response.data();
        List<CreditTransactionDto> transactions = d.transactions().stream()
                .map(CreditTransactionDto::from)
                .toList();
        return new CreditTransactionsApiResponse(
                response.success(),
                response.message(),
                new CreditTransactionsDataDto(
                        transactions,
                        d.total(),
                        d.limit(),
                        d.offset()));
    }
}
