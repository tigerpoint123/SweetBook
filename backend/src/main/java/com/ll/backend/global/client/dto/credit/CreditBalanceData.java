package com.ll.backend.global.client.dto.credit;

import com.ll.backend.domain.credit.dto.CreditsDataDto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CreditBalanceData(
        String accountUid,
        Long balance,
        String currency,
        String env,
        String createdAt,
        String updatedAt
) {
    public CreditBalanceData {
        accountUid = normalizeText(accountUid);
        currency = normalizeText(currency);
        env = normalizeText(env);
        createdAt = normalizeText(createdAt);
        updatedAt = normalizeText(updatedAt);
    }

    public static CreditBalanceData fromNullable(CreditBalanceData data) {
        return data != null ? data : new CreditBalanceData(null, null, null, null, null, null);
    }

    public CreditsDataDto toDto() {
        return new CreditsDataDto(accountUid, balance, currency, env, createdAt, updatedAt);
    }

    private static String normalizeText(String value) {
        return value != null && !value.isBlank() ? value.trim() : null;
    }
}
