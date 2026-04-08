package com.ll.backend.global.client.dto.order;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GetOrdersResponse(
        boolean success,
        String message,
        Map<String, Object> data
) {
}
