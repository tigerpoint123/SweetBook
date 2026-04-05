package com.ll.backend.domain.order.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 책(Sweetbook 북) 구매 기록. Order API는 추후 반영.
 */
@Entity
@Table(name = "book_order")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BookOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    /** Sweetbook 북 UID */
    @Column(name = "book_uid", nullable = false, length = 128)
    private String bookUid;

    @Builder
    public BookOrder(Long memberId, String bookUid) {
        this.memberId = memberId;
        this.bookUid = bookUid;
    }
}
