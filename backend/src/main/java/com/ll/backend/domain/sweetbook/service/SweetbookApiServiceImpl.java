package com.ll.backend.domain.sweetbook.service;

import com.ll.backend.domain.photo.entity.Photo;
import com.ll.backend.domain.photo.repository.BookCoverRepository;
import com.ll.backend.domain.photo.repository.PhotoRepository;
import com.ll.backend.domain.photo.repository.SelectedPhotoRepository;
import com.ll.backend.domain.photo.service.PhotoService;
import com.ll.backend.domain.sweetbook.entity.SweetbookBook;
import com.ll.backend.domain.sweetbook.repository.SweetbookBookRepository;
import com.ll.backend.domain.sweetbook.support.SweetbookCreateResponseParser;
import com.ll.backend.domain.sweetbook.vo.MyBookItemResponse;
import com.ll.backend.global.client.SweetbookApiClient;
import com.ll.backend.global.storage.LocalPhotoStorage;
import com.ll.backend.global.client.dto.AddBookContentsRequest;
import com.ll.backend.global.client.dto.BookGalleryData;
import com.ll.backend.global.client.dto.BookPhotoItem;
import com.ll.backend.global.client.dto.BookPhotosData;
import com.ll.backend.global.client.dto.BooksListData;
import com.ll.backend.global.client.dto.CreateBookRequest;
import com.ll.backend.global.client.dto.PhotoUploadData;
import com.ll.backend.global.client.dto.PhotoUploadOutcome;
import com.ll.backend.global.client.dto.BookListItem;
import com.ll.backend.global.client.dto.SweetbookApiEnvelope;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Slf4j
public class SweetbookApiServiceImpl implements SweetbookApiService {

    private final SweetbookApiClient sweetbookApiClient;
    private final PhotoRepository photoRepository;
    private final PhotoService photoService;
    private final BookCoverRepository bookCoverRepository;
    private final SelectedPhotoRepository selectedPhotoRepository;
    private final LocalPhotoStorage localPhotoStorage;
    private final SweetbookBookRepository sweetbookBookRepository;

    @Override
    public SweetbookApiEnvelope<BooksListData> listBooks(
            Integer limit,
            Integer offset,
            String pdfStatusIn,
            String createdFrom,
            String createdTo,
            boolean finalizedOnly) {
        SweetbookApiEnvelope<BooksListData> envelope =
                sweetbookApiClient.listBooks(limit, offset, pdfStatusIn, createdFrom, createdTo);
        if (envelope == null || envelope.data() == null) {
            return envelope;
        }
        BooksListData data = envelope.data();
        List<BookListItem> books = data.books();
        if (books == null) {
            return new SweetbookApiEnvelope<>(
                    envelope.success(),
                    new BooksListData(List.of(), data.total(), data.limit(), data.offset()));
        }
        if (finalizedOnly) {
            Set<String> finalizedUids =
                    sweetbookBookRepository.findAllByFinalizedAtIsNotNull().stream()
                            .map(SweetbookBook::getBookUid)
                            .collect(Collectors.toSet());
            List<BookListItem> filtered =
                    books.stream().filter(b -> finalizedUids.contains(b.bookUid())).toList();
            return new SweetbookApiEnvelope<>(
                    envelope.success(),
                    new BooksListData(filtered, filtered.size(), data.limit(), data.offset()));
        }
        return envelope;
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
    public SweetbookApiEnvelope<BookGalleryData> getBookPhotosAfterLocalLookup(String bookUid) {
        Objects.requireNonNull(bookUid, "bookUid");
        if (!sweetbookBookRepository.existsByBookUid(bookUid) && !photoRepository.existsByBookUid(bookUid)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "로컬에 등록된 책이 아니거나 해당 북 사진 이력이 없습니다.");
        }
        SweetbookApiEnvelope<BookPhotosData> env = sweetbookApiClient.getBookPhotos(bookUid);
        BookPhotosData d = env.data();
        if (d == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY, "Sweetbook 사진 목록 본문이 비어 있습니다.");
        }
        boolean finalized =
                sweetbookBookRepository
                        .findByBookUid(bookUid)
                        .map(b -> b.getFinalizedAt() != null)
                        .orElse(false);
        List<BookPhotoItem> photos =
                d.photos() != null ? d.photos() : Collections.emptyList();
        BookGalleryData merged = new BookGalleryData(photos, d.totalCount(), finalized);
        return new SweetbookApiEnvelope<>(env.success(), merged);
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
        assertMemberOwnsBookAndNotFinalized(bookUid, memberId);
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

    @Override
    @Transactional
    public Map<String, Object> deleteBookPhoto(String bookUid, String fileName, Long memberId) {
        Objects.requireNonNull(bookUid, "bookUid");
        Objects.requireNonNull(fileName, "fileName");
        Objects.requireNonNull(memberId, "memberId");
        if (memberId <= 0) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        assertMemberOwnsBookAndNotFinalized(bookUid, memberId);
        String name = fileName.trim();
        if (name.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "fileName이 비어 있습니다.");
        }
        Photo photo =
                photoRepository
                        .findByBookUidAndSweetbookFileName(bookUid, name)
                        .orElseThrow(
                                () -> new ResponseStatusException(
                                        HttpStatus.NOT_FOUND, "해당 북·파일명의 로컬 사진 기록이 없습니다."));
        Map<String, Object> response = sweetbookApiClient.deleteBookPhoto(bookUid, name);
        bookCoverRepository
                .findByBookUid(bookUid)
                .filter(bc -> bc.getPhotoId().equals(photo.getId()))
                .ifPresent(bookCoverRepository::delete);
        try {
            localPhotoStorage.deleteIfUnderUploadRoot(photo.getLocalPath());
            localPhotoStorage.deleteIfUnderUploadRoot(photo.getBlurLocalPath());
        } catch (Exception e) {
            log.warn("로컬 사진 파일 삭제 중 오류 bookUid={} photoId={}", bookUid, photo.getId(), e);
        }
        selectedPhotoRepository.deleteByPhotoId(photo.getId());
        photoRepository.delete(photo);
        photoService.recomputeSampleFlagsForBook(bookUid);
        return response;
    }

    @Override
    @Transactional
    public Map<String, Object> finalizeBook(String bookUid, Long memberId) {
        Objects.requireNonNull(bookUid, "bookUid");
        Objects.requireNonNull(memberId, "memberId");
        if (memberId <= 0) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        if (!sweetbookBookRepository.existsByBookUidAndMemberId(bookUid, memberId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "이 북을 최종화할 권한이 없습니다.");
        }
        Map<String, Object> response = sweetbookApiClient.finalizeBook(bookUid);
        if (Boolean.TRUE.equals(response.get("success"))) {
            Instant at = parseFinalizedAtFromSweetbookResponse(response);
            sweetbookBookRepository
                    .findByBookUidAndMemberId(bookUid, memberId)
                    .ifPresent(
                            book -> {
                                book.markFinalized(at);
                                sweetbookBookRepository.save(book);
                            });
        }
        return response;
    }

    private void assertMemberOwnsBookAndNotFinalized(String bookUid, Long memberId) {
        SweetbookBook book =
                sweetbookBookRepository
                        .findByBookUidAndMemberId(bookUid, memberId)
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.FORBIDDEN, "이 북을 수정할 권한이 없습니다."));
        if (book.getFinalizedAt() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 최종화된 책입니다");
        }
    }

    private static Instant parseFinalizedAtFromSweetbookResponse(Map<String, Object> response) {
        Object data = response.get("data");
        if (data instanceof Map<?, ?> m) {
            Object fa = m.get("finalizedAt");
            if (fa instanceof String s && !s.isBlank()) {
                try {
                    return Instant.parse(s);
                } catch (Exception ignored) {
                    // fall through
                }
            }
        }
        return Instant.now();
    }

    private void persistPhotoAfterUpload(String bookUid, PhotoUploadOutcome outcome) {
        SweetbookApiEnvelope<PhotoUploadData> env = outcome.envelope();
        if (outcome.originalLocalPath() == null
                || outcome.blurLocalPath() == null
                || env == null
                || !env.success()
                || env.data() == null) {
            return;
        }
        PhotoUploadData d = env.data();
        Photo saved =
                photoRepository.save(
                        Photo.builder()
                                .localPath(outcome.originalLocalPath())
                                .blurLocalPath(outcome.blurLocalPath())
                                .originalName(d.originalName())
                                .sweetbookFileName(d.fileName())
                                .bookUid(bookUid)
                                .size(d.size())
                                .mimeType(d.mimeType())
                                .uploadedAt(d.uploadedAt())
                                .hash(d.hash())
                                .isDuplicate(d.isDuplicate())
                                .sample(false)
                                .build());
        saved.assignApiUrlsIfBlank();
        photoRepository.save(saved);
        photoService.recomputeSampleFlagsForBook(bookUid);
    }
}
