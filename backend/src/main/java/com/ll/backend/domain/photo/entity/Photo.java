package com.ll.backend.domain.photo.entity;

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
@Table(name = "photo")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Photo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 로컬 서버 저장 경로
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
            boolean isDuplicate) {
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
