package com.ll.backend.domain.order.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderShippingRequest(
        @NotBlank String recipientName,
        @NotBlank String recipientPhone,
        @NotBlank String postalCode,
        @NotBlank String address1,
        String address2,
        String memo) {}
