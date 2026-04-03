package com.ll.backend.domain.sweetbook.controller;

import com.ll.backend.domain.sweetbook.service.SweetbookApiService;
import com.ll.backend.global.client.dto.CreateBookRequest;
import com.ll.backend.global.client.dto.PhotoUploadData;
import com.ll.backend.global.client.dto.SweetbookApiEnvelope;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/sweetbook")
@RequiredArgsConstructor
public class SweetbookApiController {

    private final SweetbookApiService sweetbookApiService;

    @GetMapping("/books")
    public Map<String, Object> books() {
        return sweetbookApiService.getBooks();
    }

    @PostMapping(value = "/books", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> createBook(
            @Valid @RequestBody CreateBookRequest body
    ) {
        return sweetbookApiService.createBook(body);
    }

    @PostMapping(
            value = "/books/{bookUid}/photos",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public SweetbookApiEnvelope<PhotoUploadData> uploadPhotos(
            @PathVariable String bookUid,
            @RequestParam("files") List<MultipartFile> files
    ) {
        return sweetbookApiService.uploadPhotos(bookUid, files);
    }
}
