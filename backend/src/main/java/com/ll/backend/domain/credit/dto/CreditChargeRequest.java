package com.ll.backend.domain.credit.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Positive;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CreditChargeRequest(
        @Positive int amount,
        String memo
) {
    public CreditChargeRequest {
        memo = memo == null ? "" : memo.trim();
    }
}
