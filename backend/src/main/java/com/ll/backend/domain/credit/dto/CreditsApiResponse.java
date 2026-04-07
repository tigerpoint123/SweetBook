package com.ll.backend.domain.credit.dto;

public record CreditsApiResponse(boolean success, String message, CreditsDataDto data) {}
