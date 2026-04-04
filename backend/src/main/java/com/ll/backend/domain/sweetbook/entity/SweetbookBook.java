package com.ll.backend.domain.sweetbook.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "sweetbook_book")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SweetbookBook {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(nullable = false, length = 128)
    private String bookUid;

    @Column(nullable = false)
    private Instant createdAt;

    @Builder
    public SweetbookBook(Long memberId, String bookUid, Instant createdAt) {
        this.memberId = memberId;
        this.bookUid = bookUid;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }
}
