package com.ll.backend.global.client.dto.order;

import com.ll.backend.domain.order.dto.OrderShippingRequest;

public record CreateOrderShippingPayload(
        String recipientName,
        String recipientPhone,
        String postalCode,
        String address1,
        String address2,
        String memo
) {
    public static CreateOrderShippingPayload of(OrderShippingRequest shipping) {
        String recipientName = shipping != null ? shipping.recipientName() : null;
        String recipientPhone = shipping != null ? shipping.recipientPhone() : null;
        String postalCode = shipping != null ? shipping.postalCode() : null;
        String address1 = shipping != null ? shipping.address1() : null;
        String address2 = shipping != null ? shipping.address2() : null;
        String memo = shipping != null ? shipping.memo() : null;
        String safeAddress2 = address2 == null || address2.isBlank() ? "" : address2.trim();
        String safeMemo = memo == null || memo.isBlank() ? "" : memo.trim();
        return new CreateOrderShippingPayload(
                recipientName != null ? recipientName.trim() : "",
                recipientPhone != null ? recipientPhone.trim() : "",
                postalCode != null ? postalCode.trim() : "",
                address1 != null ? address1.trim() : "",
                safeAddress2,
                safeMemo);
    }
}
