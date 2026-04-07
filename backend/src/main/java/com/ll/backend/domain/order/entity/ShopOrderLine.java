package com.ll.backend.domain.order.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "shop_order_line")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ShopOrderLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private ShopOrder order;

    @Column(name = "book_uid", nullable = false, length = 128)
    private String bookUid;

    @Column(nullable = false)
    private int quantity;

    public ShopOrderLine(ShopOrder order, String bookUid, int quantity) {
        this.order = order;
        this.bookUid = bookUid;
        this.quantity = quantity;
    }

    void assignOrder(ShopOrder order) {
        this.order = order;
    }
}
