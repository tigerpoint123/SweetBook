package com.ll.backend.domain.sweetbook.controller;

import com.ll.backend.domain.member.service.MemberService;
import com.ll.backend.domain.sweetbook.dto.FinalizeBookRequest;
import com.ll.backend.domain.sweetbook.service.SweetbookApiService;
import com.ll.backend.domain.sweetbook.vo.MyBookItemResponse;
import com.ll.backend.global.client.dto.book.AddBookContentsRequest;
import com.ll.backend.global.client.dto.book.AddBookContentsResponse;
import com.ll.backend.global.client.dto.book.BookGalleryData;
import com.ll.backend.global.client.dto.book.BookPhotosData;
import com.ll.backend.global.client.dto.book.BooksListData;
import com.ll.backend.global.client.dto.book.CreateBookRequest;
import com.ll.backend.global.client.dto.book.CreateBookResponseData;
import com.ll.backend.global.client.dto.photo.PhotoUploadData;
import com.ll.backend.global.client.dto.book.SweetbookResponse;
import com.ll.backend.global.client.dto.book.SweetbookApiEnvelope;
import com.ll.backend.global.client.dto.book.SweetbookApiResponse;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@RestController
@RequestMapping("/api/sweetbook")
@RequiredArgsConstructor
@Slf4j
public class SweetbookApiController {
    private static final String SESSION_COOKIE_NAME = "SESSION";
    @Value("${sweetbook.cover.template-uid}")
    private String coverTemplateUid;

    private final SweetbookApiService sweetbookApiService;
    private final MemberService memberService;


    @GetMapping(value = "/books", produces = MediaType.APPLICATION_JSON_VALUE)
    public SweetbookApiEnvelope<BooksListData> listBooks(
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Integer offset,
            @RequestParam(required = false) String pdfStatusIn,
            @RequestParam(required = false) String createdFrom,
            @RequestParam(required = false) String createdTo
    ) {
        return sweetbookApiService.listBooks(limit, offset, pdfStatusIn, createdFrom, createdTo);
    }

    @GetMapping(value = "/my-books", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<MyBookItemResponse>> listMyBooks(
            @CookieValue(name = SESSION_COOKIE_NAME, required = false) String sessionId
    ) {
        if (sessionId == null || sessionId.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Optional<Long> memberIdOpt = memberService.getMemberIdBySessionId(sessionId);
        if (memberIdOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(sweetbookApiService.listBooksOwnedByMember(memberIdOpt.get()));
    }

    @PostMapping(value = "/books", consumes = MediaType.APPLICATION_JSON_VALUE)
    public SweetbookApiResponse<CreateBookResponseData> createBook(
            @Valid @RequestBody CreateBookRequest body,
            @CookieValue(name = SESSION_COOKIE_NAME, required = false) String sessionId) {
        Optional<Long> memberId =
                (sessionId != null && !sessionId.isBlank())
                        ? memberService.getMemberIdBySessionId(sessionId)
                        : Optional.empty();
        return sweetbookApiService.createBook(body, memberId);
    }

    @DeleteMapping(value = "/books/{bookUid}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> deleteBook(
            @PathVariable String bookUid,
            @CookieValue(name = SESSION_COOKIE_NAME, required = false) String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Optional<Long> memberIdOpt = memberService.getMemberIdBySessionId(sessionId);
        if (memberIdOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            SweetbookResponse result = sweetbookApiService.deleteBook(bookUid, memberIdOpt.get());
            return ResponseEntity.ok(result);
        } catch (WebClientResponseException e) {
            String responseBody = e.getResponseBodyAsString();
            if (responseBody.isBlank()) {
                return ResponseEntity.status(e.getStatusCode()).build();
            }
            return ResponseEntity.status(e.getStatusCode())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(responseBody);
        }
    }

    @GetMapping(value = "/books/{bookUid}/gallery", produces = MediaType.APPLICATION_JSON_VALUE)
    public SweetbookApiEnvelope<BookGalleryData> getBookGallery(@PathVariable String bookUid) {
        return sweetbookApiService.getBookPhotosAfterLocalLookup(bookUid);
    }

    @GetMapping(value = "/books/{bookUid}/photos", produces = MediaType.APPLICATION_JSON_VALUE)
    public SweetbookApiEnvelope<BookPhotosData> getBookPhotos(@PathVariable String bookUid) {
        return sweetbookApiService.getBookPhotos(bookUid);
    }

    @PostMapping(
            value = "/books/{bookUid}/contents",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> addBookContents(
            @PathVariable String bookUid,
            @Valid @RequestBody AddBookContentsRequest body,
            @CookieValue(name = SESSION_COOKIE_NAME, required = false) String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Optional<Long> memberIdOpt = memberService.getMemberIdBySessionId(sessionId);
        if (memberIdOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            AddBookContentsResponse result = sweetbookApiService.addBookContents(bookUid, body, memberIdOpt.get());
            return ResponseEntity.ok(result);
        } catch (WebClientResponseException e) {
            String responseBody = e.getResponseBodyAsString();
            log.warn(
                    "addBookContents Sweetbook 오류 bookUid={}, status={}, 서버응답body={}",
                    bookUid,
                    e.getStatusCode().value(),
                    responseBody);
            if (responseBody.isBlank()) {
                return ResponseEntity.status(e.getStatusCode()).build();
            }
            return ResponseEntity.status(e.getStatusCode())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(responseBody);
        }
    }

    @PostMapping(
            value = "/books/{bookUid}/finalization",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> finalizeBook(
            @PathVariable String bookUid,
            @Valid @RequestBody FinalizeBookRequest body,
            @CookieValue(name = SESSION_COOKIE_NAME, required = false) String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Optional<Long> memberIdOpt = memberService.getMemberIdBySessionId(sessionId);
        if (memberIdOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            SweetbookResponse result =
                    sweetbookApiService.finalizeBook(bookUid, memberIdOpt.get(), body.price());
            return ResponseEntity.ok(result);
        } catch (WebClientResponseException e) {
            String responseBody = e.getResponseBodyAsString();
            log.warn(
                    "finalizeBook Sweetbook 오류 bookUid={}, status={}, 서버응답body={}",
                    bookUid,
                    e.getStatusCode().value(),
                    responseBody != null ? responseBody : "");
            if (responseBody == null || responseBody.isBlank()) {
                return ResponseEntity.status(e.getStatusCode()).build();
            }
            return ResponseEntity.status(e.getStatusCode())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(responseBody);
        }
    }

    @PostMapping(
            value = "/books/{bookUid}/photos",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public SweetbookApiEnvelope<PhotoUploadData> uploadPhoto(
            @PathVariable String bookUid,
            @RequestParam("file") MultipartFile file
    ) {
        log.info(
                "uploadPhoto 수신 bookUid={}, originalFilename={}, size={}, empty={}",
                bookUid,
                file != null ? file.getOriginalFilename() : null,
                file != null ? file.getSize() : 0,
                file == null || file.isEmpty());
        return sweetbookApiService.uploadPhoto(bookUid, file);
    }

    @DeleteMapping(value = "/books/{bookUid}/photos/{fileName}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> deleteBookPhoto(
            @PathVariable String bookUid,
            @PathVariable String fileName,
            @CookieValue(name = SESSION_COOKIE_NAME, required = false) String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Optional<Long> memberIdOpt = memberService.getMemberIdBySessionId(sessionId); // 여기부터
        if (memberIdOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            SweetbookResponse result =
                    sweetbookApiService.deleteBookPhoto(bookUid, fileName, memberIdOpt.get());
            return ResponseEntity.ok(result);
        } catch (WebClientResponseException e) {
            String responseBody = e.getResponseBodyAsString();
            if (responseBody == null || responseBody.isBlank()) {
                return ResponseEntity.status(e.getStatusCode()).build();
            }
            return ResponseEntity.status(e.getStatusCode())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(responseBody);
        }
    }

    @PostMapping(
            value = "/books/{bookUid}/cover",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public SweetbookResponse uploadBookCover(
            @PathVariable String bookUid,
            MultipartHttpServletRequest request
    ) {
        String parametersJson = request.getParameter("parameters");

        MultipartFile coverPhoto = request.getFile("coverPhoto");
        MultipartFile backPhoto = request.getFile("backPhoto");
        if (coverPhoto == null || coverPhoto.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "coverPhoto(앞표지 메인) 파일이 필요합니다.");
        }
        boolean hasBack = backPhoto != null && !backPhoto.isEmpty();
        log.info(
                "uploadBookCover 수신 bookUid={}, templateUid={}, coverSize={}, backPresent={}",
                bookUid,
                coverTemplateUid,
                coverPhoto.getSize(),
                hasBack);
        return sweetbookApiService.uploadBookCover(bookUid, coverTemplateUid, parametersJson, coverPhoto, backPhoto);
    }
}
