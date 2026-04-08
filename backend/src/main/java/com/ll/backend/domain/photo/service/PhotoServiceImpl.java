package com.ll.backend.domain.photo.service;

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
import com.ll.backend.global.storage.LocalPhotoStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PhotoServiceImpl implements PhotoService {
    @Value("${app.photo.upload-dir:uploads/photos}")
    private String uploadDir;

    private final PhotoRepository photoRepository;
    private final BookCoverRepository bookCoverRepository;
    private final SelectedPhotoRepository selectedPhotoRepository;
    private final SweetbookBookRepository sweetbookBookRepository;

    @Override
    public List<LocalPhotoItemResponse> list(Optional<String> bookUid, Optional<Long> viewerMemberId) {
        Map<String, Long> priceByBookUid = new HashMap<>();

        Optional<String> uidOpt = bookUid.filter(s -> !s.isBlank());
        List<Photo> photos = uidOpt.map(photoRepository::findByBookUidOrderByIdDesc)
                .orElseGet(photoRepository::findAllByOrderByIdDesc);

        if (uidOpt.isPresent()) {
            Long bookPrice =
                    sweetbookBookRepository
                            .findByBookUid(uidOpt.get())
                            .map(SweetbookBook::getPrice)
                            .orElse(null);
            return photos.stream().map(p -> toItem(p, bookPrice)).toList();
        }

        return photos.stream()
                .map(
                        p ->
                                toItem(
                                        p,
                                        priceByBookUid.computeIfAbsent(
                                                p.getBookUid(),
                                                u ->
                                                        sweetbookBookRepository
                                                                .findByBookUid(u)
                                                                .map(SweetbookBook::getPrice)
                                                                .orElse(null))))
                .toList();
    }

    @Override
    public List<LocalPhotoItemResponse> listSelectedForBook(String bookUid, Optional<Long> viewerMemberId) {
        List<LocalPhotoItemResponse> out = new ArrayList<>();

        Long bookPrice = sweetbookBookRepository.findByBookUid(bookUid)
                .map(SweetbookBook::getPrice)
                .orElse(null);

        for (SelectedPhoto sp : selectedPhotoRepository.findAllByBookUidOrderByIdAsc(bookUid)) {
            photoRepository
                    .findById(sp.getPhotoId())
                    .ifPresent(
                            p ->
                                    out.add(toItem(p, bookPrice)));
        }
        return out;
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
        return serveOriginalFile(loadPhotoOrThrow(id));
    }

    private ServedPhoto serveOriginalFile(Photo photo) {
        Path root = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path path = LocalPhotoStorage.resolveStoredPath(photo.getLocalPath(), root);
        validateUnderRoot(path);
        return buildServed(photo, path);
    }

    private Photo loadPhotoOrThrow(long id) {
        return photoRepository
                .findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "photo not found"));
    }

    private void validateUnderRoot(Path path) {
        Path root = Paths.get(uploadDir).toAbsolutePath().normalize();
        if (!path.normalize().startsWith(root)) {
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

    private LocalPhotoItemResponse toItem(Photo p, Long bookPrice) {
        long id = p.getId();
        String fileUrl = "/api/photos/" + id + "/file";
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
                bookPrice);
    }
}
