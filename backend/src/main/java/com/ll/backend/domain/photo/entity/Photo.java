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

    /** 원본 파일 경로. 업로드 루트({@code app.photo.upload-dir}) 기준 상대 경로(슬래시). 기존 절대 경로 행도 호환. */
    private String localPath;

    private String originalName;
    private String sweetbookFileName;
    private String bookUid;

    private Long size;
    private String mimeType;
    private Instant uploadedAt;
    private String hash;
    private boolean isDuplicate;

    @Builder
    public Photo(
            String localPath,
            String originalName,
            String sweetbookFileName,
            String bookUid,
            Long size,
            String mimeType,
            Instant uploadedAt,
            String hash,
            boolean isDuplicate,
            Object ignoredSample,
            Object ignoredOriginalUrl,
            Object ignoredBlurUrl,
            Object ignoredBlurLocalPath) {
        this.localPath = localPath;
        this.originalName = originalName;
        this.sweetbookFileName = sweetbookFileName;
        this.bookUid = bookUid;
        this.size = size;
        this.mimeType = mimeType;
        this.uploadedAt = uploadedAt;
        this.hash = hash;
        this.isDuplicate = isDuplicate;
    }
}
