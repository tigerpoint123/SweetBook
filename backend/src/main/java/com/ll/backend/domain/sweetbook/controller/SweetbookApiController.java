package com.ll.backend.domain.sweetbook.controller;

import com.ll.backend.domain.member.service.MemberService;
import com.ll.backend.domain.sweetbook.service.SweetbookApiService;
import com.ll.backend.domain.sweetbook.support.SweetbookCoverDefaults;
import com.ll.backend.domain.sweetbook.vo.MyBookItemResponse;
import com.ll.backend.global.client.dto.AddBookContentsRequest;
import com.ll.backend.global.client.dto.BookGalleryData;
import com.ll.backend.global.client.dto.BookPhotosData;
import com.ll.backend.global.client.dto.BooksListData;
import com.ll.backend.global.client.dto.CreateBookRequest;
import com.ll.backend.global.client.dto.PhotoUploadData;
import com.ll.backend.global.client.dto.SweetbookApiEnvelope;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import tools.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/sweetbook")
@RequiredArgsConstructor
@Slf4j
public class SweetbookApiController { // TODO : 이미지 조회 url은 없고, 내가 직접 db에 url 저장해서 볼 수 있도록 해야한다.

    private static final String SESSION_COOKIE_NAME = "SESSION";

    private final SweetbookApiService sweetbookApiService;
    private final MemberService memberService;
    private final ObjectMapper objectMapper;

    @GetMapping(value = "/books", produces = MediaType.APPLICATION_JSON_VALUE)
    public SweetbookApiEnvelope<BooksListData> listBooks(
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Integer offset,
            @RequestParam(required = false) String pdfStatusIn,
            @RequestParam(required = false) String createdFrom,
            @RequestParam(required = false) String createdTo,
            @RequestParam(required = false, defaultValue = "false") boolean finalizedOnly) {
        return sweetbookApiService.listBooks(
                limit, offset, pdfStatusIn, createdFrom, createdTo, finalizedOnly);
    }

    @GetMapping(value = "/my-books", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<MyBookItemResponse>> listMyBooks(
            @CookieValue(name = SESSION_COOKIE_NAME, required = false) String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Optional<Long> memberIdOpt = memberService.resolveMemberIdBySessionId(sessionId);
        if (memberIdOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(sweetbookApiService.listBooksOwnedByMember(memberIdOpt.get()));
    }

    @PostMapping(value = "/books", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> createBook(
            @Valid @RequestBody CreateBookRequest body,
            @CookieValue(name = SESSION_COOKIE_NAME, required = false) String sessionId) {
        Optional<Long> memberId =
                (sessionId != null && !sessionId.isBlank())
                        ? memberService.resolveMemberIdBySessionId(sessionId)
                        : Optional.empty();
        return sweetbookApiService.createBook(body, memberId);
    }

    /**
     * Sweetbook DELETE /v1/books/{bookUid} 프록시. 세션 사용자가 해당 북 생성자일 때만 허용.
     */
    @DeleteMapping(value = "/books/{bookUid}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> deleteBook(
            @PathVariable String bookUid,
            @CookieValue(name = SESSION_COOKIE_NAME, required = false) String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Optional<Long> memberIdOpt = memberService.resolveMemberIdBySessionId(sessionId);
        if (memberIdOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            Map<String, Object> result = sweetbookApiService.deleteBook(bookUid, memberIdOpt.get());
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
        Optional<Long> memberIdOpt = memberService.resolveMemberIdBySessionId(sessionId);
        if (memberIdOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            Map<String, Object> result =
                    sweetbookApiService.addBookContents(bookUid, body, memberIdOpt.get());
            try {
                log.info(
                        "addBookContents 백엔드→클라이언트 응답 bookUid={}, body={}",
                        bookUid,
                        objectMapper.writeValueAsString(result));
            } catch (Exception logEx) {
                log.info(
                        "addBookContents 백엔드→클라이언트 응답 bookUid={}, body={}",
                        bookUid,
                        result);
            }
            return ResponseEntity.ok(result);
        } catch (WebClientResponseException e) {
            String responseBody = e.getResponseBodyAsString();
            log.warn(
                    "addBookContents Sweetbook 오류 bookUid={}, status={}, 서버응답body={}",
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
            value = "/books/{bookUid}/finalization",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> finalizeBook(
            @PathVariable String bookUid,
            @CookieValue(name = SESSION_COOKIE_NAME, required = false) String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Optional<Long> memberIdOpt = memberService.resolveMemberIdBySessionId(sessionId);
        if (memberIdOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            Map<String, Object> result =
                    sweetbookApiService.finalizeBook(bookUid, memberIdOpt.get());
            try {
                log.info(
                        "finalizeBook 백엔드→클라이언트 응답 bookUid={}, body={}",
                        bookUid,
                        objectMapper.writeValueAsString(result));
            } catch (Exception logEx) {
                log.info(
                        "finalizeBook 백엔드→클라이언트 응답 bookUid={}, body={}",
                        bookUid,
                        result);
            }
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
        Optional<Long> memberIdOpt = memberService.resolveMemberIdBySessionId(sessionId);
        if (memberIdOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            Map<String, Object> result =
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

    /**
     * Sweetbook POST /v1/books/{bookUid}/cover 프록시 (multipart).
     * <ul>
     *   <li>{@code templateUid} — 생략 시 {@link SweetbookCoverDefaults#TEMPLATE_UID}</li>
     *   <li>{@code parameters} — JSON 문자열 (예: title, author)</li>
     *   <li>{@code coverPhoto} — 앞표지 메인(필수)</li>
     *   <li>{@code backPhoto} — 선택(템플릿이 뒷표지 파트를 따로 요구할 때만; 일부 템플릿은 두 파트를 같은 키로 처리해 중복 오류가 남)</li>
     * </ul>
     */
    @PostMapping(
            value = "/books/{bookUid}/cover",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Map<String, Object> uploadBookCover(
            @PathVariable String bookUid, MultipartHttpServletRequest request) {
        String templateUid = Optional.ofNullable(request.getParameter("templateUid"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .orElse(SweetbookCoverDefaults.TEMPLATE_UID);
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
                templateUid,
                coverPhoto.getSize(),
                hasBack);
        return sweetbookApiService.uploadBookCover(bookUid, templateUid, parametersJson, coverPhoto, backPhoto);
    }
}
