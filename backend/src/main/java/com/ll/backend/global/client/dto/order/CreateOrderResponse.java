package com.ll.backend.global.client.dto.order;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CreateOrderResponse(
        boolean success,
        String message,
        Map<String, Object> data
) {
}
