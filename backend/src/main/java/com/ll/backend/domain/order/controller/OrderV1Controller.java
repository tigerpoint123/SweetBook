package com.ll.backend.domain.order.controller;

import com.ll.backend.domain.member.service.MemberService;
import com.ll.backend.domain.order.dto.CreateOrderRequest;
import com.ll.backend.domain.order.dto.OrderCancelRequest;
import com.ll.backend.domain.order.dto.OrderEstimateRequest;
import com.ll.backend.domain.order.dto.OrderShippingUpdateRequest;
import com.ll.backend.domain.order.dto.OrdersListApiResponse;
import com.ll.backend.domain.order.service.OrderV1Service;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
@Slf4j
public class OrderV1Controller {

    private static final String SESSION_COOKIE_NAME = "SESSION";

    private final OrderV1Service orderV1Service;
    private final MemberService memberService;

    @GetMapping(value = "/orders", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<OrdersListApiResponse> listOrders(
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset,
            @CookieValue(name = SESSION_COOKIE_NAME, required = false) String sessionId
    ) {
        if (sessionId == null || sessionId.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        var memberIdOpt = memberService.resolveMemberIdBySessionId(sessionId);
        if (memberIdOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        int lim = Math.min(Math.max(limit, 1), 100);
        int off = Math.max(offset, 0);
        OrdersListApiResponse res = orderV1Service.listOrders(memberIdOpt.get(), lim, off);
        return ResponseEntity.ok(res);
    }

    @GetMapping(value = "/orders/{orderUid}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getOrderDetail(
            @PathVariable String orderUid,
            @CookieValue(name = SESSION_COOKIE_NAME, required = false) String sessionId
    ) {
        if (sessionId == null || sessionId.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Optional<Long> memberIdOpt = memberService.resolveMemberIdBySessionId(sessionId);
        if (memberIdOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            Map<String, Object> result = orderV1Service.getOrderDetail(memberIdOpt.get(), orderUid);
            return ResponseEntity.ok(result);
        } catch (WebClientResponseException e) {
            String responseBody = e.getResponseBodyAsString();
            if (responseBody.isBlank()) {
                return ResponseEntity.status(e.getStatusCode()).build();
            }
            return ResponseEntity.status(e.getStatusCode())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(responseBody);
        }
    }

    @PostMapping(
            value = "/orders/{orderUid}/cancel",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> cancelOrder(
            @PathVariable String orderUid,
            @Valid @RequestBody OrderCancelRequest body,
            @CookieValue(name = SESSION_COOKIE_NAME, required = false) String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Optional<Long> memberIdOpt = memberService.resolveMemberIdBySessionId(sessionId);
        if (memberIdOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            Map<String, Object> result =
                    orderV1Service.cancelOrder(memberIdOpt.get(), orderUid, body.reason());
            return ResponseEntity.ok(result);
        } catch (WebClientResponseException e) {
            String responseBody = e.getResponseBodyAsString();
            if (responseBody == null || responseBody.isBlank()) {
                return ResponseEntity.status(e.getStatusCode()).build();
            }
            return ResponseEntity.status(e.getStatusCode())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(responseBody);
        }
    }

    @PatchMapping(
            value = "/orders/{orderUid}/shipping",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> updateOrderShipping(
            @PathVariable String orderUid,
            @Valid @RequestBody OrderShippingUpdateRequest body,
            @CookieValue(name = SESSION_COOKIE_NAME, required = false) String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Optional<Long> memberIdOpt = memberService.resolveMemberIdBySessionId(sessionId);
        if (memberIdOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            Map<String, Object> result =
                    orderV1Service.updateOrderShipping(
                            memberIdOpt.get(), orderUid, body.recipientName(), body.address1());
            return ResponseEntity.ok(result);
        } catch (WebClientResponseException e) {
            String responseBody = e.getResponseBodyAsString();
            if (responseBody == null || responseBody.isBlank()) {
                return ResponseEntity.status(e.getStatusCode()).build();
            }
            return ResponseEntity.status(e.getStatusCode())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(responseBody);
        }
    }

    @PostMapping(
            value = "/orders",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createOrder(
            @Valid @RequestBody CreateOrderRequest body,
            @CookieValue(name = SESSION_COOKIE_NAME, required = false) String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Optional<Long> memberIdOpt = memberService.resolveMemberIdBySessionId(sessionId);
        if (memberIdOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        log.info(
                "createOrder 요청 memberId={} items={} shippingName={} extRef={}",
                memberIdOpt.get(),
                summarizeItems(body),
                body.shipping() != null ? body.shipping().recipientName() : null,
                body.externalRef());
        try {
            Map<String, Object> res = orderV1Service.createOrder(memberIdOpt.get(), body);
            log.info(
                    "createOrder 응답 memberId={} success={} orderUid={}",
                    memberIdOpt.get(),
                    res.get("success"),
                    extractOrderUid(res));
            return ResponseEntity.ok(res);
        } catch (WebClientResponseException e) {
            String responseBody = e.getResponseBodyAsString();
            if (responseBody == null || responseBody.isBlank()) {
                return ResponseEntity.status(e.getStatusCode()).build();
            }
            return ResponseEntity.status(e.getStatusCode())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(responseBody);
        }
    }

    @PostMapping(
            value = "/orders/estimate",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> estimateOrder(
            @Valid @RequestBody OrderEstimateRequest body,
            @CookieValue(name = SESSION_COOKIE_NAME, required = false) String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Optional<Long> memberIdOpt = memberService.resolveMemberIdBySessionId(sessionId);
        if (memberIdOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            Map<String, Object> result = orderV1Service.estimateOrder(memberIdOpt.get(), body);
            return ResponseEntity.ok(result);
        } catch (WebClientResponseException e) {
            String responseBody = e.getResponseBodyAsString();
            if (responseBody == null || responseBody.isBlank()) {
                return ResponseEntity.status(e.getStatusCode()).build();
            }
            return ResponseEntity.status(e.getStatusCode())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(responseBody);
        }
    }

    private static List<String> summarizeItems(CreateOrderRequest body) {
        if (body == null || body.items() == null) {
            return List.of();
        }
        return body.items().stream()
                .map(i -> i.bookUid() + "x" + i.quantity())
                .toList();
    }

    private static Object extractOrderUid(Map<String, Object> body) {
        Object data = body.get("data");
        if (data instanceof Map<?, ?> m) {
            return m.get("orderUid");
        }
        return null;
    }
}
