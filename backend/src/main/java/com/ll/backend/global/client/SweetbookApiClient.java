package com.ll.backend.global.client;

import com.ll.backend.global.client.dto.AddBookContentsRequest;
import com.ll.backend.global.client.dto.BookPhotosData;
import com.ll.backend.global.client.dto.BooksListData;
import com.ll.backend.global.client.dto.CreateBookRequest;
import com.ll.backend.global.client.dto.PhotoUploadData;
import com.ll.backend.global.client.dto.PhotoUploadOutcome;
import com.ll.backend.global.client.dto.SweetbookApiEnvelope;
import com.ll.backend.global.storage.LocalPhotoStorage;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriBuilder;

@Component
@Slf4j
public class SweetbookApiClient {
    private final WebClient sweetbookWebClient;
    private final LocalPhotoStorage localPhotoStorage;
    private final ObjectMapper objectMapper;
    private final String contentsTemplateUid;
    private final String contentsBreakBefore;

    public SweetbookApiClient(
            WebClient sweetbookWebClient,
            LocalPhotoStorage localPhotoStorage,
            ObjectMapper objectMapper,
            @Value("${sweetbook.contents.template-uid:1vuzMfUnCkXS}") String contentsTemplateUid,
            @Value("${sweetbook.contents.break-before:page}") String contentsBreakBefore) {
        this.sweetbookWebClient = sweetbookWebClient;
        this.localPhotoStorage = localPhotoStorage;
        this.objectMapper = objectMapper;
        this.contentsTemplateUid = contentsTemplateUid;
        this.contentsBreakBefore = contentsBreakBefore;
    }

    private void logSweetbookServerResponse(String operation, String context, Object responseBody) {
        try {
            String json =
                    responseBody == null ? "null" : objectMapper.writeValueAsString(responseBody);
            log.info("Sweetbook 서버 응답 {} {} body={}", operation, context, json);
        } catch (Exception e) {
            log.info("Sweetbook 서버 응답 {} {} body={}", operation, context, String.valueOf(responseBody));
        }
    }

    private void logSweetbookServerResponseRaw(String operation, String context, String rawBody) {
        String display = (rawBody == null || rawBody.isBlank()) ? "(empty)" : rawBody;
        log.info("Sweetbook 서버 응답 {} {} body={}", operation, context, display);
    }

    public SweetbookApiEnvelope<BooksListData> listBooks(
            Integer limit,
            Integer offset,
            String pdfStatusIn,
            String createdFrom,
            String createdTo) {
        try {
            SweetbookApiEnvelope<BooksListData> body = sweetbookWebClient.get()
                    .uri(uriBuilder -> buildBooksListUri(uriBuilder, limit, offset, pdfStatusIn, createdFrom, createdTo))
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<SweetbookApiEnvelope<BooksListData>>() {})
                    .block();
            logSweetbookServerResponse("listBooks", "", body);
            return body;
        } catch (WebClientResponseException e) {
            log.error(
                    "Sweetbook listBooks 실패 status={} 서버응답body={}",
                    e.getStatusCode().value(),
                    e.getResponseBodyAsString());
            throw e;
        }
    }

    private static URI buildBooksListUri(
            UriBuilder uriBuilder,
            Integer limit,
            Integer offset,
            String pdfStatusIn,
            String createdFrom,
            String createdTo) {
        UriBuilder b = uriBuilder.path("/books");
        if (limit != null) {
            int clamped = Math.min(100, Math.max(1, limit));
            b.queryParam("limit", clamped);
        }
        if (offset != null) {
            b.queryParam("offset", offset);
        }
        if (StringUtils.hasText(pdfStatusIn)) {
            b.queryParam("pdfStatusIn", pdfStatusIn);
        }
        if (StringUtils.hasText(createdFrom)) {
            b.queryParam("createdFrom", createdFrom);
        }
        if (StringUtils.hasText(createdTo)) {
            b.queryParam("createdTo", createdTo);
        }
        return b.build();
    }

    public Map<String, Object> createBook(CreateBookRequest request) {
        try {
            WebClient.RequestHeadersSpec<?> spec = sweetbookWebClient.post()
                    .uri("/books")
                    .bodyValue(request);

            Map<String, Object> body = spec
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
            logSweetbookServerResponse("createBook", "", body);
            return body;
        } catch (WebClientResponseException e) {
            log.error(
                    "Sweetbook createBook 실패 status={} 서버응답body={}",
                    e.getStatusCode().value(),
                    e.getResponseBodyAsString());
            throw e;
        }
    }

    public Map<String, Object> deleteBook(String bookUid) {
        Objects.requireNonNull(bookUid, "bookUid");
        try {
            Map<String, Object> body = sweetbookWebClient.delete()
                    .uri("/books/{bookUid}", bookUid)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
            Map<String, Object> safe = body != null ? body : Map.of();
            logSweetbookServerResponse("deleteBook", "bookUid=" + bookUid, safe);
            return safe;
        } catch (WebClientResponseException e) {
            log.error(
                    "Sweetbook deleteBook 실패 bookUid={} status={} 서버응답body={}",
                    bookUid,
                    e.getStatusCode().value(),
                    e.getResponseBodyAsString());
            throw e;
        }
    }

    /** Sweetbook POST /v1/books/{bookUid}/finalization */
    public Map<String, Object> finalizeBook(String bookUid) {
        Objects.requireNonNull(bookUid, "bookUid");
        try {
            Map<String, Object> body = sweetbookWebClient.post()
                    .uri("/books/{bookUid}/finalization", bookUid)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
            Map<String, Object> safe = body != null ? body : Map.of();
            logSweetbookServerResponse("finalizeBook", "bookUid=" + bookUid, safe);
            return safe;
        } catch (WebClientResponseException e) {
            log.error(
                    "Sweetbook finalizeBook 실패 bookUid={} status={} 서버응답body={}",
                    bookUid,
                    e.getStatusCode().value(),
                    e.getResponseBodyAsString());
            throw e;
        }
    }

    /** Sweetbook DELETE /v1/books/{bookUid}/photos/{fileName} */
    public Map<String, Object> deleteBookPhoto(String bookUid, String fileName) {
        Objects.requireNonNull(bookUid, "bookUid");
        Objects.requireNonNull(fileName, "fileName");
        String name = fileName.trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("fileName is blank");
        }
        String ctx = "bookUid=" + bookUid + " fileName=" + name;
        try {
            String raw = sweetbookWebClient.delete()
                    .uri("/books/{bookUid}/photos/{fileName}", bookUid, name)
                    .retrieve()
                    .bodyToMono(String.class)
                    .defaultIfEmpty("")
                    .block();
            logSweetbookServerResponseRaw("deleteBookPhoto", ctx, raw);
            if (raw == null || raw.isBlank()) {
                return Map.of("success", true);
            }
            try {
                return objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {});
            } catch (Exception e) {
                log.warn("Sweetbook deleteBookPhoto 응답이 JSON이 아님, success만 반환", e);
                return Map.of("success", true);
            }
        } catch (WebClientResponseException e) {
            log.error(
                    "Sweetbook deleteBookPhoto 실패 {} status={} 서버응답body={}",
                    ctx,
                    e.getStatusCode().value(),
                    e.getResponseBodyAsString());
            throw e;
        }
    }

    public SweetbookApiEnvelope<BookPhotosData> getBookPhotos(String bookUid) {
        Objects.requireNonNull(bookUid, "bookUid");
        try {
            SweetbookApiEnvelope<BookPhotosData> response = sweetbookWebClient.get()
                    .uri("/books/{bookUid}/photos", bookUid)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<SweetbookApiEnvelope<BookPhotosData>>() {})
                    .block();
            logSweetbookServerResponse("getBookPhotos", "bookUid=" + bookUid, response);
            return response;
        } catch (WebClientResponseException e) {
            log.error(
                    "Sweetbook getBookPhotos 실패 bookUid={} status={} 서버응답body={}",
                    bookUid,
                    e.getStatusCode().value(),
                    e.getResponseBodyAsString());
            throw e;
        }
    }

    public PhotoUploadOutcome uploadPhoto(String bookUid, MultipartFile file) {
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
            logSweetbookServerResponse("uploadPhoto", "bookUid=" + bookUid, response);

            String localPath = null;
            if (response != null && response.success() && response.data() != null) {
                localPath = saveUploadedPhotoLocally(bookUid, bytes, filename);
            }
            return new PhotoUploadOutcome(response, localPath);
        } catch (WebClientResponseException e) {
            log.error(
                    "Sweetbook uploadPhoto 실패 bookUid={} status={} 서버응답body={}",
                    bookUid,
                    e.getStatusCode().value(),
                    e.getResponseBodyAsString());
            throw e;
        }
    }

    public Map<String, Object> addBookContents(String bookUid, AddBookContentsRequest request) {
        Objects.requireNonNull(bookUid, "bookUid");
        Objects.requireNonNull(request, "request");
        AddBookContentsRequest.ContentsParameters params = request.parameters();
        String monthYearLabel = params.monthYearLabel();
        if (monthYearLabel.isEmpty()) {
            monthYearLabel = YearMonth.now(ZoneId.of("Asia/Seoul")).toString();
        }
        String parametersJson = toContentsTemplateParametersJson(monthYearLabel, params.photos());

        String templateUidPart =
                StringUtils.hasText(request.templateUid()) ? request.templateUid() : contentsTemplateUid;

        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("templateUid", templateUidPart);
        builder.part("parameters", parametersJson).contentType(MediaType.APPLICATION_JSON);

        log.info(
                "Sweetbook addBookContents → POST .../books/{}/contents?breakBefore={}, multipart templateUid={}, photosSize={}",
                bookUid,
                contentsBreakBefore,
                templateUidPart,
                params.photos().size());

        try {
            Map<String, Object> body = sweetbookWebClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/books/{bookUid}/contents")
                            .queryParam("breakBefore", contentsBreakBefore)
                            .build(bookUid))
                    .body(BodyInserters.fromMultipartData(builder.build()))
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
            if (body == null) {
                body = Map.of();
            }
            logSweetbookServerResponse("addBookContents", "bookUid=" + bookUid, body);
            return body;
        } catch (WebClientResponseException e) {
            log.error(
                    "Sweetbook addBookContents 실패 bookUid={} status={} 서버응답body={}",
                    bookUid,
                    e.getStatusCode().value(),
                    e.getResponseBodyAsString());
            throw e;
        }
    }

    public Map<String, Object> uploadBookCover(
            String bookUid,
            String templateUid,
            String parametersJson,
            MultipartFile coverPhoto,
            MultipartFile backPhoto) {
        Objects.requireNonNull(bookUid, "bookUid");
        Objects.requireNonNull(templateUid, "templateUid");
        if (coverPhoto == null || coverPhoto.isEmpty()) {
            throw new IllegalArgumentException("coverPhoto가 필요합니다.");
        }

        String coverName = filenameOrDefault(coverPhoto.getOriginalFilename(), "cover.jpg");
        byte[] coverBytes;
        try {
            coverBytes = coverPhoto.getBytes();
        } catch (IOException e) {
            log.error("Sweetbook uploadBookCover getBytes() 실패: {}", e.getMessage());
            throw new IllegalStateException("Failed to read multipart file bytes", e);
        }

        MediaType coverType = resolvePartMediaType(coverPhoto, coverName);
        // 템플릿 1Es0DP4oARn8 등: subtitle, dateRange 필수 — 비어 있으면 빈 문자열 키 포함 JSON
        String params =
                StringUtils.hasText(parametersJson)
                        ? parametersJson
                        : "{\"title\":\"\",\"author\":\"\",\"subtitle\":\"\",\"dateRange\":\"\"}";

        boolean sendBack = backPhoto != null && !backPhoto.isEmpty();
        log.info(
                "Sweetbook uploadBookCover → POST .../books/{}/cover, templateUid={}, coverBytes={}, backPart={}",
                bookUid,
                templateUid,
                coverBytes.length,
                sendBack);

        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("templateUid", templateUid);
        builder.part("parameters", params).contentType(MediaType.APPLICATION_JSON);
        builder.part("coverPhoto", new ByteArrayResource(coverBytes))
                .filename(coverName)
                .contentType(coverType);
        if (sendBack) {
            String backName = filenameOrDefault(backPhoto.getOriginalFilename(), "back.jpg");
            byte[] backBytes;
            try {
                backBytes = backPhoto.getBytes();
            } catch (IOException e) {
                log.error("Sweetbook uploadBookCover backPhoto getBytes() 실패: {}", e.getMessage());
                throw new IllegalStateException("Failed to read backPhoto bytes", e);
            }
            MediaType backType = resolvePartMediaType(backPhoto, backName);
            builder.part("backPhoto", new ByteArrayResource(backBytes))
                    .filename(backName)
                    .contentType(backType);
        }

        try {
            Map<String, Object> body = sweetbookWebClient.post()
                    .uri("/books/{bookUid}/cover", bookUid)
                    .body(BodyInserters.fromMultipartData(builder.build()))
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
            if (body == null) {
                body = Map.of();
            }
            logSweetbookServerResponse("uploadBookCover", "bookUid=" + bookUid, body);
            return body;
        } catch (WebClientResponseException e) {
            log.error(
                    "Sweetbook uploadBookCover 실패 bookUid={} status={} 서버응답body={}",
                    bookUid,
                    e.getStatusCode().value(),
                    e.getResponseBodyAsString());
            throw e;
        }
    }

    private static String filenameOrDefault(String original, String fallback) {
        return (original != null && !original.isBlank()) ? original : fallback;
    }

    private String saveUploadedPhotoLocally(String bookUid, byte[] bytes, String originalFilename) {
        return localPhotoStorage.save(bookUid, bytes, originalFilename);
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

    /** 템플릿 1vuzMfUnCkXS: {@code {"monthYearLabel":"2026-04","photos":["a.PNG","b.PNG"]}} */
    private static String toContentsTemplateParametersJson(String monthYearLabel, List<String> photos) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"monthYearLabel\":");
        sb.append(jsonStringLiteral(monthYearLabel));
        sb.append(",\"photos\":[");
        for (int i = 0; i < photos.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(jsonStringLiteral(photos.get(i)));
        }
        sb.append("]}");
        return sb.toString();
    }

    private static String jsonStringLiteral(String s) {
        if (s == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder(s.length() + 2);
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append('"');
        return sb.toString();
    }
}
