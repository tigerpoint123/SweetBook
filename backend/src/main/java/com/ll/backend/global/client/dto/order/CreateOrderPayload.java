package com.ll.backend.global.client.dto.order;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

public record CreateOrderPayload(
        List<CreateOrderItemPayload> items,
        CreateOrderShippingPayload shipping,
        @JsonInclude(JsonInclude.Include.NON_NULL) String externalRef
) {
}
