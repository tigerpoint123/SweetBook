package com.ll.backend.domain.credit.dto;

public record CreditsDataDto(
        String accountUid,
        Long balance,
        String currency,
        String env,
        String createdAt,
        String updatedAt) {}
