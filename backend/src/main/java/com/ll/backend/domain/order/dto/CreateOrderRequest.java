package com.ll.backend.domain.order.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CreateOrderRequest(
        @NotEmpty @Valid List<OrderLineRequest> items,
        @NotNull @Valid OrderShippingRequest shipping,
        String externalRef) {}
