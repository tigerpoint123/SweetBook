package com.ll.backend.domain.photo.service;

import com.ll.backend.domain.photo.dto.BookCoverItemResponse;
import com.ll.backend.domain.photo.dto.LocalPhotoItemResponse;
import com.ll.backend.domain.photo.dto.SaveBookCoverRequest;
import java.util.List;
import java.util.Optional;
import org.springframework.core.io.Resource;

public interface PhotoService {

    record ServedPhoto(Resource resource, String contentType) {}

    /** bookUid가 있으면 해당 북만; 순서는 photo.id 내림차순(업로드·갤러리용). */
    List<LocalPhotoItemResponse> list(Optional<String> bookUid);

    /** 책 넘김(채택)용 — selected_photo.id 오름차순(먼저 채택한 것이 앞, 가장 최근 채택이 뒤) */
    List<LocalPhotoItemResponse> listSelectedForBook(String bookUid);

    /** 이번에 채택한 photoIds를 순서대로 추가(기존 selected_photo 행은 유지) */
    void appendBookSelection(String bookUid, List<Long> photoIds);

    ServedPhoto servePhoto(long id);

    List<BookCoverItemResponse> listBookCovers();

    void saveBookCover(String bookUid, SaveBookCoverRequest request);
}
