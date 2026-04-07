package com.ll.backend.domain.photo.service;

import com.ll.backend.domain.photo.dto.BookCoverItemResponse;
import com.ll.backend.domain.photo.dto.LocalPhotoItemResponse;
import com.ll.backend.domain.photo.dto.SaveBookCoverRequest;
import java.util.List;
import java.util.Optional;
import org.springframework.core.io.Resource;

public interface PhotoService {

    record ServedPhoto(Resource resource, String contentType) {}

    List<LocalPhotoItemResponse> list(Optional<String> bookUid, Optional<Long> viewerMemberId);

    List<LocalPhotoItemResponse> listSelectedForBook(String bookUid, Optional<Long> viewerMemberId);

    /** {@code is_sample=true} 만, id 오름차순. 갤러리용 {@code fileUrl}은 항상 {@code /file}. */
    List<LocalPhotoItemResponse> listSamplePhotosForBook(String bookUid);

    /** {@code is_sample=false} 만, id 내림차순. {@code fileUrl}은 항상 원본 {@code /file}. */
    List<LocalPhotoItemResponse> listNonSamplePhotosForBook(String bookUid, Optional<Long> viewerMemberId);

    /** 해당 북에서 id 오름차순 상위 3장만 {@code is_sample=true}, 나머지 false로 맞춥니다. */
    void recomputeSampleFlagsForBook(String bookUid);

    /** 이번에 채택한 photoIds를 순서대로 추가(기존 selected_photo 행은 유지) */
    void appendBookSelection(String bookUid, List<Long> photoIds);

    /** {@code /file} — 항상 원본 바이너리. */
    ServedPhoto servePhoto(long id, Optional<Long> memberId);

    List<BookCoverItemResponse> listBookCovers();

    void saveBookCover(String bookUid, SaveBookCoverRequest request);
}
