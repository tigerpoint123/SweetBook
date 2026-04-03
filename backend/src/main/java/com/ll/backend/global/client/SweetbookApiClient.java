package com.ll.backend.global.client;

import com.ll.backend.global.client.dto.BookPhotosData;
import com.ll.backend.global.client.dto.CreateBookRequest;
import com.ll.backend.global.client.dto.PhotoUploadData;
import com.ll.backend.global.client.dto.SweetbookApiEnvelope;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Component
@Slf4j
public class SweetbookApiClient {
    private final WebClient sweetbookWebClient;

    public SweetbookApiClient(WebClient sweetbookWebClient) {
        this.sweetbookWebClient = sweetbookWebClient;
    }

    public Map<String, Object> getBooks() {
        try {
            Map<String, Object> body = sweetbookWebClient.get()
                    .uri("/books")
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                    })
                    .block();
            log.info("Sweetbook getBooks 성공, 응답 keys={}", body != null ? body.keySet() : "null");
            return body;
        } catch (WebClientResponseException e) {
            log.error(
                    "Sweetbook getBooks 실패 status={} body={}",
                    e.getStatusCode().value(),
                    e.getResponseBodyAsString());
            throw e;
        }
    }

    public Map<String, Object> createBook(CreateBookRequest request) {
        log.info(
                "Sweetbook createBook 호출 title='{}', bookSpecUid='{}', bookAuthor='{}', externalRef='{}'",
                request.title(),
                request.bookSpecUid(),
                request.bookAuthor(),
                request.externalRef());

        try {
            WebClient.RequestHeadersSpec<?> spec = sweetbookWebClient.post()
                    .uri("/books")
                    .bodyValue(request);

            Map<String, Object> body = spec
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
            log.info(
                    "Sweetbook createBook 성공, 응답 keys={}",
                    body != null ? body.keySet() : "null");
            return body;
        } catch (WebClientResponseException e) {
            log.error(
                    "Sweetbook createBook 실패 status={} body={}",
                    e.getStatusCode().value(),
                    e.getResponseBodyAsString());
            throw e;
        }
    }

    /**
     * {@code GET /v1/books/{bookUid}/photos} — 상대 경로 {@code /books/{bookUid}/photos}.
     */
    public SweetbookApiEnvelope<BookPhotosData> getBookPhotos(String bookUid) {
        Objects.requireNonNull(bookUid, "bookUid");
        try {
            SweetbookApiEnvelope<BookPhotosData> response = sweetbookWebClient.get()
                    .uri("/books/{bookUid}/photos", bookUid)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<SweetbookApiEnvelope<BookPhotosData>>() {})
                    .block();
            log.info("Sweetbook getBookPhotos 성공 bookUid={}, totalCount={}", bookUid,
                    response != null && response.data() != null ? response.data().totalCount() : "?");
            return response;
        } catch (WebClientResponseException e) {
            log.error(
                    "Sweetbook getBookPhotos 실패 status={} body={}",
                    e.getStatusCode().value(),
                    e.getResponseBodyAsString());
            throw e;
        }
    }

    /**
     * {@code POST /v1/books/{bookUid}/photos} — {@code sweetbook.api.base-url} 에 {@code /v1} 이 포함된 경우
     * 상대 경로는 {@code /books/{bookUid}/photos} 입니다. multipart 필드명은 {@code file} (단일 파일).
     */
    public SweetbookApiEnvelope<PhotoUploadData> uploadPhoto(String bookUid, MultipartFile file) {
        Objects.requireNonNull(bookUid, "bookUid");
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("이미지 파일(file) 한 개가 필요합니다.");
        }

        String filename = (file.getOriginalFilename() != null && !file.getOriginalFilename().isBlank())
                ? file.getOriginalFilename()
                : "photo.jpg";

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            log.error("Sweetbook uploadPhoto getBytes() 실패: {}", e.getMessage());
            throw new IllegalStateException("Failed to read multipart file bytes", e);
        }

        MediaType resolvedType = resolvePartMediaType(file, filename);
        log.info(
                "Sweetbook uploadPhoto → POST .../books/{}/photos, part=file, filename={}, bytes={}, Content-Type={}",
                bookUid,
                filename,
                bytes.length,
                resolvedType);

        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("file", new ByteArrayResource(bytes))
                .filename(filename)
                .contentType(resolvedType);

        try {
            SweetbookApiEnvelope<PhotoUploadData> response = sweetbookWebClient.post()
                    .uri("/books/{bookUid}/photos", bookUid)
                    .body(BodyInserters.fromMultipartData(builder.build()))
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<SweetbookApiEnvelope<PhotoUploadData>>() {})
                    .block();
            log.info("Sweetbook uploadPhoto 성공 bookUid={}, response={}", bookUid, response);
            return response;
        } catch (WebClientResponseException e) {
            log.error(
                    "Sweetbook uploadPhoto 실패 status={} body={}",
                    e.getStatusCode().value(),
                    e.getResponseBodyAsString());
            throw e;
        }
    }

    private static MediaType resolvePartMediaType(MultipartFile file, String filename) {
        String ct = file.getContentType();
        if (ct != null && !ct.isBlank()) {
            try {
                return MediaType.parseMediaType(ct);
            } catch (IllegalArgumentException ignored) {
                // fall through
            }
        }
        return MediaTypeFactory.getMediaType(filename)
                .orElse(MediaType.APPLICATION_OCTET_STREAM);
    }
}
