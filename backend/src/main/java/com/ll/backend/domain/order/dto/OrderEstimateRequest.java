package com.ll.backend.domain.order.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderEstimateRequest(@NotEmpty @Valid List<OrderLineRequest> items) {}
