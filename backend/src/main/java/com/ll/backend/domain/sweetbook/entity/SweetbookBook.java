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

    /** Sweetbook 편집 최종화 시각. null 이면 편집 가능으로 간주 */
    @Column(name = "finalized_at")
    private Instant finalizedAt;

    /** 최종화 시 설정되는 판매·표시 단가(원). */
    private Long price;

    @Builder
    public SweetbookBook(Long memberId, String bookUid, Instant createdAt, Instant finalizedAt) {
        this.memberId = memberId;
        this.bookUid = bookUid;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.finalizedAt = finalizedAt;
    }

    public void markFinalized(Instant at) {
        this.finalizedAt = at != null ? at : Instant.now();
    }

    public void setPrice(Long price) {
        this.price = price;
    }
}
