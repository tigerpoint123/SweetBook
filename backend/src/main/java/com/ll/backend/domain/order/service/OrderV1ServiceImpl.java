package com.ll.backend.domain.order.service;

import com.ll.backend.domain.order.dto.CreateOrderRequest;
import com.ll.backend.domain.order.dto.OrderEstimateRequest;
import com.ll.backend.domain.order.dto.OrderLineRequest;
import com.ll.backend.domain.order.dto.OrderShippingRequest;
import com.ll.backend.domain.order.dto.OrderSummaryItemDto;
import com.ll.backend.domain.order.dto.OrdersListApiResponse;
import com.ll.backend.domain.order.dto.OrdersListDataDto;
import com.ll.backend.domain.order.entity.BookOrder;
import com.ll.backend.domain.order.entity.ShopOrder;
import com.ll.backend.domain.order.entity.ShopOrderLine;
import com.ll.backend.domain.order.repository.OrderRepository;
import com.ll.backend.domain.order.repository.ShopOrderRepository;
import com.ll.backend.domain.sweetbook.entity.SweetbookBook;
import com.ll.backend.domain.sweetbook.repository.SweetbookBookRepository;
import com.ll.backend.global.client.SweetbookApiClient;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderV1ServiceImpl implements OrderV1Service {

    private static final String SUCCESS = "Success";

    private final ShopOrderRepository shopOrderRepository;
    private final OrderRepository orderRepository;
    private final SweetbookBookRepository sweetbookBookRepository;
    private final SweetbookApiClient sweetbookApiClient;

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> estimateOrder(Long memberId, OrderEstimateRequest request) {
        if (memberId == null || memberId <= 0) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
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
            payloadItems.add(Map.of("bookUid", uid, "quantity", line.quantity()));
        }
        return sweetbookApiClient.estimateOrder(Map.of("items", payloadItems));
    }

    @Override
    @Transactional
    public Map<String, Object> createOrder(Long memberId, CreateOrderRequest request) {
        if (memberId == null || memberId <= 0) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        OrderShippingRequest s = request.shipping();
        String ext =
                request.externalRef() != null && !request.externalRef().isBlank()
                        ? request.externalRef().trim()
                        : null;
        String a2 = s.address2() != null ? s.address2().trim() : "";
        String memo = s.memo() != null ? s.memo().trim() : "";

        List<Map<String, Object>> payloadItems = new ArrayList<>();
        Set<String> bookUids = new LinkedHashSet<>();
        for (OrderLineRequest line : request.items()) {
            String uid = line.bookUid() != null ? line.bookUid().trim() : "";
            if (uid.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "bookUid가 비었습니다.");
            }
            payloadItems.add(Map.of("bookUid", uid, "quantity", line.quantity()));
            bookUids.add(uid);
        }

        Map<String, Object> shipping =
                Map.of(
                        "recipientName", s.recipientName().trim(),
                        "recipientPhone", s.recipientPhone().trim(),
                        "postalCode", s.postalCode().trim(),
                        "address1", s.address1().trim(),
                        "address2", a2.isEmpty() ? "" : a2,
                        "memo", memo.isEmpty() ? "" : memo);

        Map<String, Object> requestBody =
                ext != null && !ext.isBlank()
                        ? Map.of("items", payloadItems, "shipping", shipping, "externalRef", ext)
                        : Map.of("items", payloadItems, "shipping", shipping);

        Map<String, Object> sweetbookResponse = sweetbookApiClient.createOrder(requestBody);
        boolean success =
                sweetbookResponse.get("success") instanceof Boolean b && b;
        if (!success) {
            return sweetbookResponse;
        }

        BigDecimal totalAmount = computeTotalAmount(request.items());
        String orderUid = newOrderUid();
        int orderStatus = OrderV1ServiceSupport.STATUS_PAID;
        Object dataObj = sweetbookResponse.get("data");
        if (dataObj instanceof Map<?, ?> dm) {
            Object uid = dm.get("orderUid");
            if (uid instanceof String sUid && !sUid.isBlank()) {
                orderUid = sUid;
            }
            Object status = dm.get("orderStatus");
            if (status instanceof Number n) {
                orderStatus = n.intValue();
            }
            Object amount = dm.get("totalAmount");
            if (amount instanceof Number n) {
                totalAmount = BigDecimal.valueOf(n.doubleValue());
            }
        }

        ShopOrder order =
                ShopOrder.builder()
                        .memberId(memberId)
                        .externalRef(ext)
                        .recipientName(s.recipientName().trim())
                        .recipientPhone(s.recipientPhone().trim())
                        .postalCode(s.postalCode().trim())
                        .address1(s.address1().trim())
                        .address2(a2.isEmpty() ? null : a2)
                        .shippingMemo(memo.isEmpty() ? null : memo)
                        .orderUid(orderUid)
                        .orderStatus(orderStatus)
                        .totalAmount(totalAmount)
                        .build();
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
                request.items() != null ? request.items().size() : 0);

        for (String uid : bookUids) {
            if (!orderRepository.existsByMemberIdAndBookUid(memberId, uid)) {
                orderRepository.save(BookOrder.builder().memberId(memberId).bookUid(uid).build());
            }
        }

        return sweetbookResponse;
    }

    @Override
    @Transactional(readOnly = true)
    public OrdersListApiResponse listOrders(Long memberId, int limit, int offset) {
        if (memberId == null || memberId <= 0) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        Map<String, Object> response = sweetbookApiClient.getOrders(limit, offset);
        boolean success = response.get("success") instanceof Boolean b && b;
        String message =
                response.get("message") instanceof String m && !m.isBlank() ? m : SUCCESS;

        long total = 0;
        int lim = Math.min(Math.max(limit, 1), 100);
        int off = Math.max(offset, 0);
        boolean hasNext = false;
        List<OrderSummaryItemDto> items = List.of();

        Object dataObj = response.get("data");
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
        return new OrdersListApiResponse(success, message, data);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getOrderDetail(Long memberId, String orderUid) {
        if (memberId == null || memberId <= 0) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        if (orderUid == null || orderUid.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "orderUid가 비었습니다.");
        }
        return sweetbookApiClient.getOrderDetail(orderUid.trim());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> cancelOrder(Long memberId, String orderUid, String reason) {
        if (memberId == null || memberId <= 0) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
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
    @Transactional(readOnly = true)
    public Map<String, Object> updateOrderShipping(
            Long memberId, String orderUid, String recipientName, String address1) {
        if (memberId == null || memberId <= 0) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
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
            return new OrderSummaryItemDto(null, 0, null, BigDecimal.ZERO, Instant.now());
        }
        String orderUid = m.get("orderUid") instanceof String s ? s : null;
        int orderStatus = m.get("orderStatus") instanceof Number n ? n.intValue() : 0;
        String orderStatusDisplay =
                m.get("orderStatusDisplay") instanceof String s ? s : null;
        BigDecimal totalAmount =
                m.get("totalAmount") instanceof Number n
                        ? BigDecimal.valueOf(n.doubleValue())
                        : BigDecimal.ZERO;
        Instant orderedAt = Instant.now();
        Object orderedAtObj = m.get("orderedAt");
        if (orderedAtObj instanceof String s && !s.isBlank()) {
            try {
                orderedAt = Instant.parse(s);
            } catch (Exception ignored) {
                // keep now fallback
            }
        }
        return new OrderSummaryItemDto(
                orderUid, orderStatus, orderStatusDisplay, totalAmount, orderedAt);
    }

    private BigDecimal computeTotalAmount(List<OrderLineRequest> items) {
        BigDecimal sum = BigDecimal.ZERO;
        for (OrderLineRequest line : items) {
            String uid = line.bookUid().trim();
            long unit =
                    sweetbookBookRepository
                            .findByBookUid(uid)
                            .map(SweetbookBook::getPrice)
                            .filter(p -> p != null && p > 0)
                            .orElse(0L);
            sum =
                    sum.add(
                            BigDecimal.valueOf(unit)
                                    .multiply(BigDecimal.valueOf(line.quantity())));
        }
        return sum;
    }

    private static String newOrderUid() {
        String hex = java.util.UUID.randomUUID().toString().replace("-", "");
        return "or_" + hex.substring(0, Math.min(16, hex.length()));
    }
}
