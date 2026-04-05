package com.ll.backend.domain.photo.entity;

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
@Table(name = "selected_photo")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SelectedPhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "book_uid", nullable = false, length = 128)
    private String bookUid;

    @Column(name = "photo_id", nullable = false)
    private Long photoId;

    @Builder
    public SelectedPhoto(String bookUid, Long photoId) {
        this.bookUid = bookUid;
        this.photoId = photoId;
    }
}
