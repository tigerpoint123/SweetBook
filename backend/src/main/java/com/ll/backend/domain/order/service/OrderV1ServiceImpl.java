package com.ll.backend.domain.order.service;

import com.ll.backend.domain.order.dto.*;
import com.ll.backend.domain.order.entity.BookOrder;
import com.ll.backend.domain.order.entity.ShopOrder;
import com.ll.backend.domain.order.entity.ShopOrderLine;
import com.ll.backend.domain.order.repository.OrderRepository;
import com.ll.backend.domain.order.repository.ShopOrderRepository;
import com.ll.backend.domain.sweetbook.entity.SweetbookBook;
import com.ll.backend.domain.sweetbook.repository.SweetbookBookRepository;
import com.ll.backend.global.client.SweetbookApiClient;
import com.ll.backend.global.client.dto.book.SweetbookResponse;
import com.ll.backend.global.client.dto.order.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderV1ServiceImpl implements OrderV1Service {

    private final ShopOrderRepository shopOrderRepository;
    private final OrderRepository orderRepository;
    private final SweetbookBookRepository sweetbookBookRepository;
    private final SweetbookApiClient sweetbookApiClient;

    @Override
    public SweetbookResponse estimateOrder(Long memberId, OrderEstimateRequest request) {
        List<Map<String, Object>> payloadItems = new ArrayList<>();

        for (OrderLineRequest line : request.items()) {
            String uid = line.bookUid() != null ? line.bookUid().trim() : "";
            if (uid.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "bookUid가 비었습니다.");
            }
            if (!sweetbookBookRepository.existsByBookUidAndMemberId(uid, memberId)) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "해당 북에 대한 견적 조회 권한이 없습니다.");
            }
            payloadItems.add(
                    Map.of(
                            "bookUid",
                            uid,
                            "quantity",
                            line.quantity()));
        }
        return sweetbookApiClient.estimateOrder(Map.of("items", payloadItems));
    }

    @Override
    public CreateOrderResponse createOrder(Long memberId, CreateOrderRequest request) {
        List<CreateOrderItemPayload> payloadItems = new ArrayList<>();

        OrderShippingRequest orderShippingRequest = request.shipping();

        for (OrderLineRequest line : request.items()) {
            String uid = line.bookUid() != null ? line.bookUid().trim() : "";
            if (uid.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "bookUid가 비었습니다.");
            }
            payloadItems.add(new CreateOrderItemPayload(uid, line.quantity()));
        }

        CreateOrderShippingPayload shipping = CreateOrderShippingPayload.of(orderShippingRequest);
        CreateOrderPayload requestBody = new CreateOrderPayload(payloadItems, shipping, request.externalRef());
        CreateOrderResponse sweetbookResponse = sweetbookApiClient.createOrder(requestBody);

        ShopOrder order = ShopOrder.create(
                memberId,
                request,
                sweetbookResponse.data(),
                OrderV1ServiceSupport.STATUS_PAID,
                computeTotalAmount(request.items()));
        for (OrderLineRequest line : request.items()) {
            order.addLine(new ShopOrderLine(order, line.bookUid().trim(), line.quantity()));
        }

        ShopOrder saved = shopOrderRepository.save(order);
        log.info(
                "createOrder 저장 memberId={} orderId={} orderUid={} totalAmount={} itemCount={}",
                memberId,
                saved.getId(),
                saved.getOrderUid(),
                saved.getTotalAmount(),
                request.items().size());

        orderRepository.saveAll(
                request.items().stream()
                        .map(line -> BookOrder.builder()
                                .memberId(memberId)
                                .bookUid(line.bookUid())
                                .quantity(line.quantity())
                                .build())
                        .toList());

        return sweetbookResponse;
    }

    @Override
    public OrdersListApiResponse listOrders(Long memberId, int limit, int offset) {
        int lim = Math.min(Math.max(limit, 1), 100);
        int off = Math.max(offset, 0);
        long total = 0;
        boolean hasNext = false;

        GetOrdersResponse response = sweetbookApiClient.getOrders(lim, off);
        String message = response.message();

        List<OrderSummaryItemDto> items = List.of();

        Object dataObj = response.data();
        if (dataObj instanceof Map<?, ?> data) {
            // Sweetbook 실제 응답: data.orders + data.pagination
            Object paginationObj = data.get("pagination");
            if (paginationObj instanceof Map<?, ?> p) {
                if (p.get("total") instanceof Number n) total = n.longValue();
                if (p.get("limit") instanceof Number n) lim = n.intValue();
                if (p.get("offset") instanceof Number n) off = n.intValue();
                if (p.get("hasNext") instanceof Boolean b) hasNext = b;
            } else {
                // 혹시 data 바로 아래에 내려오는 형태도 호환
                if (data.get("total") instanceof Number n) total = n.longValue();
                if (data.get("limit") instanceof Number n) lim = n.intValue();
                if (data.get("offset") instanceof Number n) off = n.intValue();
                if (data.get("hasNext") instanceof Boolean b) hasNext = b;
            }
            Object rowsObj = data.get("orders");
            if (!(rowsObj instanceof List<?>)) {
                rowsObj = data.get("items");
            }
            if (rowsObj instanceof List<?> rows) {
                items = rows.stream().map(this::toOrderSummaryItem).toList();
            }
        }

        OrdersListDataDto data = new OrdersListDataDto(total, lim, off, hasNext, items);
        return new OrdersListApiResponse(response.success(), message, data);
    }

    @Override
    public GetOrderDetailResponse getOrderDetail(Long memberId, String orderUid) {
        if (orderUid == null || orderUid.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "orderUid가 비었습니다.");
        }
        return sweetbookApiClient.getOrderDetail(orderUid.trim());
    }

    @Override
    public OrderCancelResponse cancelOrder(Long memberId, String orderUid, String reason) {
        if (orderUid == null || orderUid.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "orderUid가 비었습니다.");
        }
        String reasonText = reason == null ? "" : reason.trim();
        if (reasonText.length() > 500) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "취소 사유는 500자 이하여야 합니다.");
        }
        return sweetbookApiClient.cancelOrder(orderUid.trim(), reasonText);
    }

    @Override
    public SweetbookResponse updateOrderShipping(
            Long memberId, String orderUid, String recipientName, String address1) {
        if (orderUid == null || orderUid.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "orderUid가 비었습니다.");
        }
        String name = recipientName == null ? "" : recipientName.trim();
        String addr = address1 == null ? "" : address1.trim();
        if (name.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "recipientName은 필수입니다.");
        }
        if (addr.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "address1은 필수입니다.");
        }
        return sweetbookApiClient.updateOrderShipping(orderUid.trim(), name, addr);
    }

    private OrderSummaryItemDto toOrderSummaryItem(Object row) {
        if (!(row instanceof Map<?, ?> m)) {
            return new OrderSummaryItemDto(null, 0, null, 0L, Instant.now());
        }
        String orderUid = m.get("orderUid") instanceof String s ? s : null;
        int orderStatus = m.get("orderStatus") instanceof Number n ? n.intValue() : 0;
        String orderStatusDisplay =
                m.get("orderStatusDisplay") instanceof String s ? s : null;
        long totalAmount =
                m.get("totalAmount") instanceof Number n
                        ? n.longValue()
                        : 0L;
        Instant orderedAt = Instant.now();
        Object orderedAtObj = m.get("orderedAt");
        if (orderedAtObj instanceof String s && !s.isBlank()) {
            try {
                orderedAt = Instant.parse(s);
            } catch (Exception ignored) {
            }
        }
        return new OrderSummaryItemDto(
                orderUid, orderStatus, orderStatusDisplay, totalAmount, orderedAt);
    }

    private long computeTotalAmount(List<OrderLineRequest> items) {
        long sum = 0L;
        for (OrderLineRequest line : items) {
            String uid = line.bookUid().trim();
            long unit =
                    sweetbookBookRepository
                            .findByBookUid(uid)
                            .map(SweetbookBook::getPrice)
                            .filter(p -> p > 0)
                            .orElse(0L);
            sum += unit * line.quantity();
        }
        return sum;
    }

}
