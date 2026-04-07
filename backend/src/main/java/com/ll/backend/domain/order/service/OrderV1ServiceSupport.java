package com.ll.backend.domain.order.service;

import com.ll.backend.domain.order.entity.ShopOrder;

final class OrderV1ServiceSupport {

    static final int STATUS_PAID = 20;

    private OrderV1ServiceSupport() {}

    static String orderStatusDisplay(int code) {
        if (code == STATUS_PAID) {
            return "결제완료";
        }
        return "기타(" + code + ")";
    }

    static String resolveOrderUid(ShopOrder order) {
        if (order.getOrderUid() != null && !order.getOrderUid().isBlank()) {
            return order.getOrderUid();
        }
        return "or_legacy_" + order.getId();
    }
}
