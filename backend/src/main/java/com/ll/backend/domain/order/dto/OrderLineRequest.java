package com.ll.backend.domain.order.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderLineRequest(@NotBlank String bookUid, @Min(1) int quantity) {}
