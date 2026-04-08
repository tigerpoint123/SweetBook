package com.ll.backend.domain.order.entity;

import com.ll.backend.domain.order.dto.CreateOrderRequest;
import com.ll.backend.domain.order.dto.OrderShippingRequest;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "shop_order")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ShopOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "external_ref", length = 256)
    private String externalRef;

    @Column(name = "recipient_name", nullable = false, length = 128)
    private String recipientName;

    @Column(name = "recipient_phone", nullable = false, length = 64)
    private String recipientPhone;

    @Column(name = "postal_code", nullable = false, length = 32)
    private String postalCode;

    @Column(name = "address1", nullable = false, length = 512)
    private String address1;

    @Column(name = "address2", length = 512)
    private String address2;

    @Column(name = "shipping_memo", length = 512)
    private String shippingMemo;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "order_uid", unique = true, length = 64)
    private String orderUid;

    @Column(name = "order_status", nullable = false, columnDefinition = "integer default 20")
    private int orderStatus = 20;

    @Column(name = "total_amount", precision = 14, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ShopOrderLine> lines = new ArrayList<>();

    @Builder
    public ShopOrder(
            Long memberId,
            String externalRef,
            String recipientName,
            String recipientPhone,
            String postalCode,
            String address1,
            String address2,
            String shippingMemo,
            String orderUid,
            Integer orderStatus,
            BigDecimal totalAmount) {
        this.memberId = memberId;
        this.externalRef = externalRef;
        this.recipientName = recipientName;
        this.recipientPhone = recipientPhone;
        this.postalCode = postalCode;
        this.address1 = address1;
        this.address2 = address2;
        this.shippingMemo = shippingMemo;
        this.createdAt = Instant.now();
        this.orderUid = orderUid;
        this.orderStatus = orderStatus != null ? orderStatus : 20;
        this.totalAmount = totalAmount != null ? totalAmount : BigDecimal.ZERO;
    }

    public void addLine(ShopOrderLine line) {
        lines.add(line);
        line.assignOrder(this);
    }

    public static ShopOrder create(
            Long memberId,
            CreateOrderRequest request,
            Object responseData,
            int defaultOrderStatus,
            long fallbackTotalAmount) {
        String externalRef = request != null && request.externalRef() != null
                ? request.externalRef().trim()
                : null;
        OrderShippingRequest shipping = request != null ? request.shipping() : null;
        String recipientName = shipping != null ? shipping.recipientName() : "";
        String recipientPhone = shipping != null ? shipping.recipientPhone() : "";
        String postalCode = shipping != null ? shipping.postalCode() : "";
        String address1 = shipping != null ? shipping.address1() : "";
        String address2 = shipping != null ? shipping.address2() : "";
        String shippingMemo = shipping != null ? shipping.memo() : "";

        String orderUid = newOrderUid();
        int orderStatus = defaultOrderStatus;
        long totalAmount = fallbackTotalAmount;
        if (responseData instanceof Map<?, ?> data) {
            Object uid = data.get("orderUid");
            if (uid instanceof String sUid && !sUid.isBlank()) {
                orderUid = sUid;
            }
            Object status = data.get("orderStatus");
            if (status instanceof Number n) {
                orderStatus = n.intValue();
            }
            Object amount = data.get("totalAmount");
            if (amount instanceof Number n) {
                totalAmount = n.longValue();
            }
        }

        return ShopOrder.builder()
                .memberId(memberId)
                .externalRef(externalRef)
                .recipientName(recipientName != null ? recipientName.trim() : "")
                .recipientPhone(recipientPhone != null ? recipientPhone.trim() : "")
                .postalCode(postalCode != null ? postalCode.trim() : "")
                .address1(address1 != null ? address1.trim() : "")
                .address2(address2 == null || address2.isBlank() ? null : address2)
                .shippingMemo(shippingMemo == null || shippingMemo.isBlank() ? null : shippingMemo)
                .orderUid(orderUid)
                .orderStatus(orderStatus)
                .totalAmount(BigDecimal.valueOf(totalAmount))
                .build();
    }

    private static String newOrderUid() {
        String hex = java.util.UUID.randomUUID().toString().replace("-", "");
        return "or_" + hex.substring(0, Math.min(16, hex.length()));
    }
}
