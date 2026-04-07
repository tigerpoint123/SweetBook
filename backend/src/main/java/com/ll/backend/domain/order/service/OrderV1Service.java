package com.ll.backend.domain.order.service;

import com.ll.backend.domain.order.dto.CreateOrderRequest;
import com.ll.backend.domain.order.dto.OrderEstimateRequest;
import com.ll.backend.domain.order.dto.OrdersListApiResponse;
import java.util.Map;

public interface OrderV1Service {

    Map<String, Object> createOrder(Long memberId, CreateOrderRequest request);

    Map<String, Object> estimateOrder(Long memberId, OrderEstimateRequest request);

    OrdersListApiResponse listOrders(Long memberId, int limit, int offset);

    Map<String, Object> getOrderDetail(Long memberId, String orderUid);

    Map<String, Object> cancelOrder(Long memberId, String orderUid, String reason);

    Map<String, Object> updateOrderShipping(
            Long memberId, String orderUid, String recipientName, String address1);
}
