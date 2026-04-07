package com.ll.backend.domain.order.entity;

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
}
