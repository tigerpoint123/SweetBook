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

    /** {@code is_sample=false} 만, id 내림차순. {@code fileUrl}은 조회자에 따라 {@code /file} 또는 {@code /blur}. */
    List<LocalPhotoItemResponse> listNonSamplePhotosForBook(String bookUid, Optional<Long> viewerMemberId);

    /** 해당 북에서 id 오름차순 상위 3장만 {@code is_sample=true}, 나머지 false로 맞춥니다. */
    void recomputeSampleFlagsForBook(String bookUid);

    /** 이번에 채택한 photoIds를 순서대로 추가(기존 selected_photo 행은 유지) */
    void appendBookSelection(String bookUid, List<Long> photoIds);

    /**
     * {@code /file}. 소유자·구매자·샘플은 원본, 그 외 비샘플은 블러(최종화 여부와 동일 규칙).
     */
    ServedPhoto servePhoto(long id, Optional<Long> memberId);

    /** 원본 파일. 최종화 후에는 샘플·소유자·구매자만 */
    ServedPhoto servePhotoOriginal(long id, Optional<Long> memberId);

    /** 블러 처리본만 (항상 허용) */
    ServedPhoto servePhotoBlur(long id);

    List<BookCoverItemResponse> listBookCovers();

    void saveBookCover(String bookUid, SaveBookCoverRequest request);
}
