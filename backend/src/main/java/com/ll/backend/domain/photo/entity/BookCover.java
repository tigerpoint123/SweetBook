package com.ll.backend.domain.photo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "book_cover",
        uniqueConstraints = @UniqueConstraint(name = "uk_book_cover_book_uid", columnNames = "book_uid"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BookCover {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "book_uid", nullable = false, length = 128)
    private String bookUid;

    @Column(name = "photo_id", nullable = false)
    private Long photoId;

    @Column(nullable = false, length = 1024)
    private String subtitle;

    @Column(name = "date_range", nullable = false, length = 512)
    private String dateRange;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Builder
    public BookCover(String bookUid, Long photoId, String subtitle, String dateRange, Instant updatedAt) {
        this.bookUid = bookUid;
        this.photoId = photoId;
        this.subtitle = subtitle;
        this.dateRange = dateRange;
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public void replacePhotoAndMeta(Long newPhotoId, String newSubtitle, String newDateRange) {
        this.photoId = newPhotoId;
        this.subtitle = newSubtitle;
        this.dateRange = newDateRange;
        this.updatedAt = Instant.now();
    }
}
