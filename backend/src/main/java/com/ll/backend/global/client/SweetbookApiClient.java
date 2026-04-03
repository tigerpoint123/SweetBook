package com.ll.backend.global.client;

import com.ll.backend.global.client.dto.CreateBookRequest;
import com.ll.backend.global.client.dto.PhotoUploadData;
import com.ll.backend.global.client.dto.SweetbookApiEnvelope;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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

    public SweetbookApiEnvelope<PhotoUploadData> uploadPhotos(String bookUid, List<MultipartFile> files) {
        Objects.requireNonNull(bookUid, "bookUid");
        if (files == null || files.stream().noneMatch(f -> f != null && !f.isEmpty())) {
            throw new IllegalArgumentException("At least one non-empty file is required");
        }
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }

            // Sweetbook upload은 multipart part에 filename/content-type이 포함되길 기대하는 경우가 많습니다.
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.isBlank()) {
                originalFilename = "photo";
            }
            final String filename = originalFilename;

            byte[] bytes;
            try {
                bytes = file.getBytes();
            } catch (IOException e) {
                throw new IllegalStateException("Failed to read multipart file bytes", e);
            }

            String contentType = file.getContentType();
            ByteArrayResource resource =
                    new ByteArrayResource(bytes) {
                        @Override
                        public String getFilename() {
                            return filename;
                        }
                    };

            HttpHeaders partHeaders = new HttpHeaders();
            if (contentType != null && !contentType.isBlank()) {
                partHeaders.setContentType(MediaType.parseMediaType(contentType));
            }
            builder.part("file", new HttpEntity<>(resource, partHeaders));
        }

        try {
            return sweetbookWebClient.post()
                    .uri("/books/{bookUid}/photos", bookUid)
                    .body(BodyInserters.fromMultipartData(builder.build()))
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<SweetbookApiEnvelope<PhotoUploadData>>() {
                    })
                    .block();
        } catch (WebClientResponseException e) {
            log.error(
                    "Sweetbook uploadPhotos 실패 status={} body={}",
                    e.getStatusCode().value(),
                    e.getResponseBodyAsString());
            throw e;
        }
    }
}
