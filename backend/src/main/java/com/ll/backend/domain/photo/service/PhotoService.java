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

    /** 이번에 채택한 photoIds를 순서대로 추가(기존 selected_photo 행은 유지) */
    void appendBookSelection(String bookUid, List<Long> photoIds);

    /** {@code /file} — 항상 원본 바이너리. */
    ServedPhoto servePhoto(long id, Optional<Long> memberId);

    List<BookCoverItemResponse> listBookCovers();

    void saveBookCover(String bookUid, SaveBookCoverRequest request);
}
