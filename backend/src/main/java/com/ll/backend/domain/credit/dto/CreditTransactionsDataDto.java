package com.ll.backend.domain.credit.dto;

import java.util.List;

public record CreditTransactionsDataDto(
        List<CreditTransactionDto> transactions,
        Long total,
        Integer limit,
        Integer offset) {}

