package com.ll.backend.domain.photo.entity;

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
import lombok.Setter;

@Entity
@Table(name = "photo")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Photo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 원본 파일 절대 경로 ({@code .../bookUid/original/...}) */
    private String localPath;

    /** 과거 블러 복제본 경로(더 이상 생성하지 않음, 기존 행 호환용). */
    @Column(length = 2048)
    private String blurLocalPath;

    private String originalName;
    private String sweetbookFileName;
    private String bookUid;

    private Long size;
    private String mimeType;
    private Instant uploadedAt;
    private String hash;
    private boolean isDuplicate;

    /** 결제·소유자 없이 최종화 책에서 원본 노출 대상(콘텐츠 추가 시 선정) */
    @Column(name = "is_sample", nullable = false)
    private boolean sample;

    @Column(length = 512)
    private String originalUrl;

    @Column(length = 512)
    private String blurUrl;

    @Builder
    public Photo(
            String localPath,
            String blurLocalPath,
            String originalName,
            String sweetbookFileName,
            String bookUid,
            Long size,
            String mimeType,
            Instant uploadedAt,
            String hash,
            boolean isDuplicate,
            Boolean sample,
            String originalUrl,
            String blurUrl) {
        this.localPath = localPath;
        this.blurLocalPath = blurLocalPath;
        this.originalName = originalName;
        this.sweetbookFileName = sweetbookFileName;
        this.bookUid = bookUid;
        this.size = size;
        this.mimeType = mimeType;
        this.uploadedAt = uploadedAt;
        this.hash = hash;
        this.isDuplicate = isDuplicate;
        this.sample = sample != null && sample;
        this.originalUrl = originalUrl;
        this.blurUrl = blurUrl;
    }

    public void assignApiUrlsIfBlank() {
        if (this.id == null) {
            return;
        }
        if (this.originalUrl == null || this.originalUrl.isBlank()) {
            this.originalUrl = "/api/photos/" + this.id + "/file";
        }
    }
}
