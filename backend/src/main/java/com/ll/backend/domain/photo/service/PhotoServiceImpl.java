package com.ll.backend.domain.photo.service;

import com.ll.backend.domain.photo.dto.BookCoverItemResponse;
import com.ll.backend.domain.photo.dto.LocalPhotoItemResponse;
import com.ll.backend.domain.photo.dto.SaveBookCoverRequest;
import com.ll.backend.domain.photo.entity.BookCover;
import com.ll.backend.domain.photo.entity.Photo;
import com.ll.backend.domain.photo.repository.BookCoverRepository;
import com.ll.backend.domain.photo.repository.PhotoRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    private final PhotoRepository photoRepository;
    private final BookCoverRepository bookCoverRepository;

    @Value("${app.photo.upload-dir:uploads/photos}")
    private String uploadDir;

    @Override
    public List<LocalPhotoItemResponse> list(Optional<String> bookUid) {
        List<Photo> photos =
                bookUid.filter(s -> !s.isBlank())
                        .map(photoRepository::findByBookUidOrderByIdDesc)
                        .orElseGet(photoRepository::findAllByOrderByIdDesc);
        return photos.stream().map(this::toItem).toList();
    }

    @Override
    public ServedPhoto servePhoto(long id) {
        Photo photo = photoRepository
                .findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "photo not found"));
        Path path = Paths.get(photo.getLocalPath()).normalize();
        Path root = Paths.get(uploadDir).toAbsolutePath().normalize();
        if (!path.startsWith(root) || !Files.isRegularFile(path)) {
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

    private LocalPhotoItemResponse toItem(Photo p) {
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
                "/api/photos/" + p.getId() + "/file");
    }
}
