package com.ll.backend.domain.photo.controller;

import com.ll.backend.domain.photo.dto.BookCoverItemResponse;
import com.ll.backend.domain.photo.dto.LocalPhotoItemResponse;
import com.ll.backend.domain.photo.dto.SaveBookCoverRequest;
import com.ll.backend.domain.photo.service.PhotoService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/photos")
@RequiredArgsConstructor
public class PhotoController {

    private final PhotoService photoService;

    @GetMapping
    public List<LocalPhotoItemResponse> list(@RequestParam(required = false) String bookUid) {
        return photoService.list(Optional.ofNullable(bookUid));
    }

    @GetMapping("/books/{bookUid}")
    public List<LocalPhotoItemResponse> listByBook(@PathVariable String bookUid) {
        return photoService.list(Optional.of(bookUid));
    }

    @GetMapping(value = "/book-covers", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<BookCoverItemResponse> listBookCovers() {
        return photoService.listBookCovers();
    }

    @PostMapping(
            value = "/books/{bookUid}/book-cover",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Void> saveBookCover(
            @PathVariable String bookUid, @Valid @RequestBody SaveBookCoverRequest body) {
        photoService.saveBookCover(bookUid, body);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/file")
    public ResponseEntity<Resource> file(@PathVariable Long id) {
        PhotoService.ServedPhoto served = photoService.servePhoto(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, served.contentType())
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=3600")
                .body(served.resource());
    }
}
