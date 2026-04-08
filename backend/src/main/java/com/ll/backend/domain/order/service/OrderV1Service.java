package com.ll.backend.domain.order.service;

import com.ll.backend.domain.order.dto.CreateOrderRequest;
import com.ll.backend.domain.order.dto.OrderEstimateRequest;
import com.ll.backend.domain.order.dto.OrdersListApiResponse;
import com.ll.backend.global.client.dto.order.OrderCancelResponse;
import com.ll.backend.global.client.dto.order.CreateOrderResponse;
import com.ll.backend.global.client.dto.order.GetOrderDetailResponse;
import com.ll.backend.global.client.dto.book.SweetbookResponse;

public interface OrderV1Service {

    CreateOrderResponse createOrder(Long memberId, CreateOrderRequest request);

    SweetbookResponse estimateOrder(Long memberId, OrderEstimateRequest request);

    OrdersListApiResponse listOrders(Long memberId, int limit, int offset);

    GetOrderDetailResponse getOrderDetail(Long memberId, String orderUid);

    OrderCancelResponse cancelOrder(Long memberId, String orderUid, String reason);

    SweetbookResponse updateOrderShipping(
            Long memberId, String orderUid, String recipientName, String address1);
}
