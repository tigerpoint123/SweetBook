package com.ll.backend.domain.photo.service;

import com.ll.backend.domain.order.repository.OrderRepository;
import com.ll.backend.domain.photo.dto.BookCoverItemResponse;
import com.ll.backend.domain.photo.dto.LocalPhotoItemResponse;
import com.ll.backend.domain.photo.dto.SaveBookCoverRequest;
import com.ll.backend.domain.photo.entity.BookCover;
import com.ll.backend.domain.photo.entity.Photo;
import com.ll.backend.domain.photo.entity.SelectedPhoto;
import com.ll.backend.domain.photo.repository.BookCoverRepository;
import com.ll.backend.domain.photo.repository.PhotoRepository;
import com.ll.backend.domain.photo.repository.SelectedPhotoRepository;
import com.ll.backend.domain.sweetbook.entity.SweetbookBook;
import com.ll.backend.domain.sweetbook.repository.SweetbookBookRepository;
import com.ll.backend.global.image.ImageBlurUtil;
import com.ll.backend.global.storage.LocalPhotoStorage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class PhotoServiceImpl implements PhotoService {

    private static final int SAMPLE_PHOTO_COUNT = 3;

    private final PhotoRepository photoRepository;
    private final BookCoverRepository bookCoverRepository;
    private final SelectedPhotoRepository selectedPhotoRepository;
    private final SweetbookBookRepository sweetbookBookRepository;
    private final OrderRepository orderRepository;

    @Value("${app.photo.upload-dir:uploads/photos}")
    private String uploadDir;

    @Override
    public List<LocalPhotoItemResponse> list(Optional<String> bookUid, Optional<Long> viewerMemberId) {
        List<Photo> photos =
                bookUid.filter(s -> !s.isBlank())
                        .map(photoRepository::findByBookUidOrderByIdDesc)
                        .orElseGet(photoRepository::findAllByOrderByIdDesc);
        return photos.stream()
                .map(p -> toItem(p, fileUrlShouldPointToBlur(p, viewerMemberId)))
                .toList();
    }

    @Override
    public List<LocalPhotoItemResponse> listSamplePhotosForBook(String bookUid) {
        Objects.requireNonNull(bookUid, "bookUid");
        String uid = bookUid.trim();
        if (uid.isEmpty()) {
            return List.of();
        }
        return photoRepository.findSamplesByBookUidOrderByIdAsc(uid).stream()
                .map(p -> toItem(p, false))
                .toList();
    }

    @Override
    public List<LocalPhotoItemResponse> listNonSamplePhotosForBook(String bookUid, Optional<Long> viewerMemberId) {
        Objects.requireNonNull(bookUid, "bookUid");
        String uid = bookUid.trim();
        if (uid.isEmpty()) {
            return List.of();
        }
        return photoRepository.findNonSamplesByBookUidOrderByIdDesc(uid).stream()
                .map(p -> toItem(p, fileUrlShouldPointToBlur(p, viewerMemberId)))
                .toList();
    }

    @Override
    public List<LocalPhotoItemResponse> listSelectedForBook(String bookUid, Optional<Long> viewerMemberId) {
        Objects.requireNonNull(bookUid, "bookUid");
        String uid = bookUid.trim();
        if (uid.isEmpty()) {
            return List.of();
        }
        List<LocalPhotoItemResponse> out = new ArrayList<>();
        for (SelectedPhoto sp : selectedPhotoRepository.findAllByBookUidOrderByIdAsc(uid)) {
            photoRepository
                    .findById(sp.getPhotoId())
                    .ifPresent(p -> out.add(toItem(p, fileUrlShouldPointToBlur(p, viewerMemberId))));
        }
        return out;
    }

    @Override
    @Transactional
    public void recomputeSampleFlagsForBook(String bookUid) {
        if (bookUid == null || bookUid.isBlank()) {
            return;
        }
        List<Photo> asc = photoRepository.findByBookUidOrderByIdAsc(bookUid.trim());
        for (int i = 0; i < asc.size(); i++) {
            boolean wantSample = i < SAMPLE_PHOTO_COUNT;
            Photo p = asc.get(i);
            if (p.isSample() != wantSample) {
                p.setSample(wantSample);
                photoRepository.save(p);
            }
        }
    }

    /**
     * 목록·썸네일용 {@code fileUrl}: 소유자·구매자는 항상 {@code /file}. 그 외에는 {@code is_sample=false} 인 사진만
     * {@code /blur}({@code blurLocalPath} 바이너리), 샘플 3장은 미리보기용 {@code /file}.
     */
    private boolean fileUrlShouldPointToBlur(Photo p, Optional<Long> viewerMemberId) {
        if (!listShouldUseBlurFileUrl(p.getBookUid(), viewerMemberId)) {
            return false;
        }
        return !p.isSample();
    }

    /**
     * 비로그인, 또는 해당 북 소유자가 아니면서 {@code book_order} 구매 기록도 없으면 “제한된 조회자”로 간주합니다.
     */
    private boolean listShouldUseBlurFileUrl(String photoBookUid, Optional<Long> viewerMemberId) {
        if (photoBookUid == null || photoBookUid.isBlank()) {
            return true;
        }
        String uid = photoBookUid.trim();
        if (viewerMemberId.isEmpty()) {
            return true;
        }
        long mid = viewerMemberId.get();
        if (sweetbookBookRepository.existsByBookUidAndMemberId(uid, mid)) {
            return false;
        }
        return !orderRepository.existsByMemberIdAndBookUid(mid, uid);
    }

    @Override
    @Transactional
    public void appendBookSelection(String bookUid, List<Long> photoIds) {
        Objects.requireNonNull(bookUid, "bookUid");
        String uid = bookUid.trim();
        if (uid.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "bookUid is blank");
        }
        List<Long> ids = photoIds != null ? photoIds : List.of();
        for (Long pid : ids) {
            if (pid == null || pid <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "유효하지 않은 photo id");
            }
            Photo photo =
                    photoRepository
                            .findById(pid)
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "photo not found"));
            if (!uid.equals(photo.getBookUid())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "해당 사진은 이 북(bookUid)에 속하지 않습니다.");
            }
            selectedPhotoRepository.save(SelectedPhoto.builder().bookUid(uid).photoId(pid).build());
        }
    }

    @Override
    public ServedPhoto servePhoto(long id, Optional<Long> memberId) {
        Photo photo = loadPhotoOrThrow(id);
        Path path = resolvePathForViewer(photo, memberId);
        return buildServed(photo, path);
    }

    @Override
    public ServedPhoto servePhotoOriginal(long id, Optional<Long> memberId) {
        Photo photo = loadPhotoOrThrow(id);
        if (!canAccessOriginalBinary(photo, memberId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "원본 이미지에 접근할 권한이 없습니다.");
        }
        Path path = Paths.get(photo.getLocalPath()).normalize();
        validateUnderRoot(path);
        return buildServed(photo, path);
    }

    @Override
    public ServedPhoto servePhotoBlur(long id) {
        Photo photo = loadPhotoOrThrow(id);
        ensureBlurVariantOnDisk(photo);
        String blur = photo.getBlurLocalPath();
        if (blur == null || blur.isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "blur image missing");
        }
        Path path = Paths.get(blur).normalize();
        validateUnderRoot(path);
        if (!Files.isRegularFile(path)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "blur file missing");
        }
        return buildServed(photo, path);
    }

    private Photo loadPhotoOrThrow(long id) {
        return photoRepository
                .findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "photo not found"));
    }

    private Path resolvePathForViewer(Photo photo, Optional<Long> memberId) {
        Optional<SweetbookBook> bookOpt = sweetbookBookRepository.findByBookUid(photo.getBookUid());
        if (shouldServeOriginalForFileEndpoint(photo, memberId, bookOpt)) {
            Path p = Paths.get(photo.getLocalPath()).normalize();
            validateUnderRoot(p);
            return p;
        }
        ensureBlurVariantOnDisk(photo);
        String blur = photo.getBlurLocalPath();
        if (blur == null || blur.isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "blur image missing");
        }
        Path bp = Paths.get(blur).normalize();
        validateUnderRoot(bp);
        if (!Files.isRegularFile(bp)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "blur file missing");
        }
        return bp;
    }

    /** {@code GET .../file} — 최종화 전·후 공통: 샘플·소유자·구매자만 원본, 그 외 비샘플은 블러 경로로 유도. */
    private boolean shouldServeOriginalForFileEndpoint(
            Photo photo, Optional<Long> memberId, Optional<SweetbookBook> bookOpt) {
        if (photo.isSample()) {
            return true;
        }
        if (memberId.isEmpty()) {
            return false;
        }
        long mid = memberId.get();
        boolean owner =
                bookOpt.map(b -> b.getMemberId().equals(mid)).orElse(false);
        if (owner) {
            return true;
        }
        return orderRepository.existsByMemberIdAndBookUid(mid, photo.getBookUid());
    }

    private boolean canAccessOriginalBinary(Photo photo, Optional<Long> memberId) {
        Optional<SweetbookBook> bookOpt = sweetbookBookRepository.findByBookUid(photo.getBookUid());
        boolean finalized = bookOpt.map(b -> b.getFinalizedAt() != null).orElse(false);
        if (!finalized) {
            return true;
        }
        if (photo.isSample()) {
            return true;
        }
        if (memberId.isEmpty()) {
            return false;
        }
        long mid = memberId.get();
        boolean owner = bookOpt.map(b -> b.getMemberId().equals(mid)).orElse(false);
        if (owner) {
            return true;
        }
        return orderRepository.existsByMemberIdAndBookUid(mid, photo.getBookUid());
    }

    private void ensureBlurVariantOnDisk(Photo photo) {
        if (photo.getBlurLocalPath() != null && !photo.getBlurLocalPath().isBlank()) {
            if (Files.isRegularFile(Paths.get(photo.getBlurLocalPath()))) {
                return;
            }
        }
        Path orig = Paths.get(photo.getLocalPath()).normalize();
        if (!Files.isRegularFile(orig)) {
            return;
        }
        Path blurTarget = resolveBlurTargetPath(orig).normalize();
        try {
            Files.createDirectories(blurTarget.getParent());
            ImageBlurUtil.blurToFileOrCopy(orig, blurTarget);
            photo.setBlurLocalPath(blurTarget.toAbsolutePath().toString().replace('\\', '/'));
            photoRepository.save(photo);
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "블러 이미지를 준비할 수 없습니다.");
        }
    }

    static Path resolveBlurTargetPath(Path originalFile) {
        Path parent = originalFile.getParent();
        if (parent == null) {
            return originalFile.resolveSibling(LocalPhotoStorage.SUBDIR_BLUR)
                    .resolve(originalFile.getFileName());
        }
        if (LocalPhotoStorage.SUBDIR_ORIGINAL.equals(parent.getFileName().toString())) {
            return parent.getParent()
                    .resolve(LocalPhotoStorage.SUBDIR_BLUR)
                    .resolve(originalFile.getFileName());
        }
        return parent.resolve(LocalPhotoStorage.SUBDIR_BLUR).resolve(originalFile.getFileName());
    }

    private void validateUnderRoot(Path path) {
        Path root = Paths.get(uploadDir).toAbsolutePath().normalize();
        if (!path.toAbsolutePath().normalize().startsWith(root)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "invalid path");
        }
    }

    private ServedPhoto buildServed(Photo photo, Path path) {
        if (!Files.isRegularFile(path)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "photo file missing");
        }
        String mime = photo.getMimeType();
        String contentType = (mime != null && !mime.isBlank()) ? mime : "application/octet-stream";
        return new ServedPhoto(new FileSystemResource(path), contentType);
    }

    @Override
    public List<BookCoverItemResponse> listBookCovers() {
        return bookCoverRepository.findAllByOrderByUpdatedAtDesc().stream()
                .map(bc -> new BookCoverItemResponse(
                        bc.getBookUid(),
                        bc.getPhotoId(),
                        "/api/photos/" + bc.getPhotoId() + "/file",
                        bc.getSubtitle(),
                        bc.getDateRange(),
                        bc.getUpdatedAt()))
                .toList();
    }

    @Override
    @Transactional
    public void saveBookCover(String bookUid, SaveBookCoverRequest request) {
        Objects.requireNonNull(bookUid, "bookUid");
        String uid = bookUid.trim();
        if (uid.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "bookUid is blank");
        }
        Photo photo = photoRepository
                .findById(request.photoId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "photo not found"));
        if (!uid.equals(photo.getBookUid())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "해당 사진은 이 북(bookUid)에 속하지 않습니다.");
        }
        String subtitle =
                request.subtitle() != null ? request.subtitle().trim() : "";
        String dateRange =
                request.dateRange() != null ? request.dateRange().trim() : "";
        bookCoverRepository
                .findByBookUid(uid)
                .ifPresentOrElse(
                        existing -> existing.replacePhotoAndMeta(request.photoId(), subtitle, dateRange),
                        () -> bookCoverRepository.save(BookCover.builder()
                                .bookUid(uid)
                                .photoId(request.photoId())
                                .subtitle(subtitle)
                                .dateRange(dateRange)
                                .build()));
    }

    private LocalPhotoItemResponse toItem(Photo p, boolean fileUrlPointsToBlur) {
        long id = p.getId();
        String fileUrl =
                fileUrlPointsToBlur
                        ? "/api/photos/" + id + "/blur"
                        : "/api/photos/" + id + "/file";
        return new LocalPhotoItemResponse(
                p.getId(),
                p.getBookUid(),
                p.getOriginalName(),
                p.getSweetbookFileName(),
                p.getSize(),
                p.getMimeType(),
                p.getUploadedAt(),
                p.getHash(),
                p.isDuplicate(),
                fileUrl,
                p.isSample(),
                nonBlankOrDefault(p.getOriginalUrl(), "/api/photos/" + id + "/original"),
                nonBlankOrDefault(p.getBlurUrl(), "/api/photos/" + id + "/blur"));
    }

    private static String nonBlankOrDefault(String v, String def) {
        return v != null && !v.isBlank() ? v : def;
    }
}
