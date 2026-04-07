package com.ll.backend.domain.order.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderShippingUpdateRequest(
        @NotBlank String recipientName, @NotBlank String address1) {}
