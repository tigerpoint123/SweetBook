package com.ll.backend.domain.credit.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CreditChargeRequest(int amount, String memo) {}
