package com.ll.backend.domain.sweetbook.service;

import com.ll.backend.domain.photo.entity.Photo;
import com.ll.backend.domain.photo.repository.PhotoRepository;
import com.ll.backend.domain.sweetbook.entity.SweetbookBook;
import com.ll.backend.domain.sweetbook.repository.SweetbookBookRepository;
import com.ll.backend.domain.sweetbook.support.SweetbookCreateResponseParser;
import com.ll.backend.domain.sweetbook.vo.MyBookItemResponse;
import com.ll.backend.global.client.SweetbookApiClient;
import com.ll.backend.global.client.dto.AddBookContentsRequest;
import com.ll.backend.global.client.dto.BookPhotosData;
import com.ll.backend.global.client.dto.BooksListData;
import com.ll.backend.global.client.dto.CreateBookRequest;
import com.ll.backend.global.client.dto.PhotoUploadData;
import com.ll.backend.global.client.dto.PhotoUploadOutcome;
import com.ll.backend.global.client.dto.SweetbookApiEnvelope;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class SweetbookApiServiceImpl implements SweetbookApiService {

    private final SweetbookApiClient sweetbookApiClient;
    private final PhotoRepository photoRepository;
    private final SweetbookBookRepository sweetbookBookRepository;

    @Override
    public SweetbookApiEnvelope<BooksListData> listBooks(
            Integer limit,
            Integer offset,
            String pdfStatusIn,
            String createdFrom,
            String createdTo) {
        SweetbookApiEnvelope<BooksListData> envelope =
                sweetbookApiClient.listBooks(limit, offset, pdfStatusIn, createdFrom, createdTo);
        if (envelope == null || envelope.data() == null || envelope.data().books() != null) {
            return envelope;
        }
        BooksListData data = envelope.data();
        return new SweetbookApiEnvelope<>(
                envelope.success(),
                new BooksListData(List.of(), data.total(), data.limit(), data.offset()));
    }

    @Override
    public Map<String, Object> createBook(CreateBookRequest request, Optional<Long> memberId) {
        Map<String, Object> response = sweetbookApiClient.createBook(request);
        memberId.filter(id -> id != null && id > 0)
                .ifPresent(
                        id -> SweetbookCreateResponseParser.extractBookUid(response)
                                .ifPresent(uid -> sweetbookBookRepository.save(
                                        SweetbookBook.builder().memberId(id).bookUid(uid).build())));
        return response;
    }

    @Override
    public List<MyBookItemResponse> listBooksOwnedByMember(Long memberId) {
        Objects.requireNonNull(memberId, "memberId");
        if (memberId <= 0) {
            return List.of();
        }
        return sweetbookBookRepository.findAllByMemberIdOrderByIdDesc(memberId).stream()
                .map(MyBookItemResponse::from)
                .toList();
    }

    @Override
    public SweetbookApiEnvelope<BookPhotosData> getBookPhotos(String bookUid) {
        return sweetbookApiClient.getBookPhotos(bookUid);
    }

    @Override
    public SweetbookApiEnvelope<BookPhotosData> getBookPhotosAfterLocalLookup(String bookUid) {
        Objects.requireNonNull(bookUid, "bookUid");
        if (!sweetbookBookRepository.existsByBookUid(bookUid) && !photoRepository.existsByBookUid(bookUid)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "로컬에 등록된 책이 아니거나 해당 북 사진 이력이 없습니다.");
        }
        return sweetbookApiClient.getBookPhotos(bookUid);
    }

    @Override
    @Transactional
    public SweetbookApiEnvelope<PhotoUploadData> uploadPhoto(String bookUid, MultipartFile file) {
        PhotoUploadOutcome outcome = sweetbookApiClient.uploadPhoto(bookUid, file);
        persistPhotoAfterUpload(bookUid, outcome);
        return outcome.envelope();
    }

    @Override
    public Map<String, Object> uploadBookCover(
            String bookUid,
            String templateUid,
            String parametersJson,
            MultipartFile coverPhoto,
            MultipartFile backPhoto) {
        return sweetbookApiClient.uploadBookCover(bookUid, templateUid, parametersJson, coverPhoto, backPhoto);
    }

    @Override
    public Map<String, Object> addBookContents(String bookUid, AddBookContentsRequest request, Long memberId) {
        Objects.requireNonNull(bookUid, "bookUid");
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(memberId, "memberId");
        if (memberId <= 0) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        if (!sweetbookBookRepository.existsByBookUidAndMemberId(bookUid, memberId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "이 북의 콘텐츠를 추가할 권한이 없습니다.");
        }
        return sweetbookApiClient.addBookContents(bookUid, request);
    }

    @Override
    @Transactional
    public Map<String, Object> deleteBook(String bookUid, Long memberId) {
        Objects.requireNonNull(bookUid, "bookUid");
        Objects.requireNonNull(memberId, "memberId");
        if (memberId <= 0) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        if (!sweetbookBookRepository.existsByBookUidAndMemberId(bookUid, memberId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "이 북을 삭제할 권한이 없습니다.");
        }
        Map<String, Object> response = sweetbookApiClient.deleteBook(bookUid);
        if (Boolean.TRUE.equals(response.get("success"))) {
            sweetbookBookRepository.deleteByBookUidAndMemberId(bookUid, memberId);
        }
        return response;
    }

    private void persistPhotoAfterUpload(String bookUid, PhotoUploadOutcome outcome) {
        SweetbookApiEnvelope<PhotoUploadData> env = outcome.envelope();
        if (outcome.localPath() == null || env == null || !env.success() || env.data() == null) {
            return;
        }
        PhotoUploadData d = env.data();
        photoRepository.save(
                Photo.builder()
                        .localPath(outcome.localPath())
                        .originalName(d.originalName())
                        .sweetbookFileName(d.fileName())
                        .bookUid(bookUid)
                        .size(d.size())
                        .mimeType(d.mimeType())
                        .uploadedAt(d.uploadedAt())
                        .hash(d.hash())
                        .isDuplicate(d.isDuplicate())
                        .build());
    }
}
