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

    @Column(name = "book_uid", nullable = false, length = 128)
    private String bookUid;

    @Builder
    public BookOrder(Long memberId, String bookUid) {
        this.memberId = memberId;
        this.bookUid = bookUid;
    }
}
