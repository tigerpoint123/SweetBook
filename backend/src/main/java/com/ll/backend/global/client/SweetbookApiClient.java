package com.ll.backend.global.client;

import com.ll.backend.global.client.dto.book.*;
import com.ll.backend.global.client.dto.credit.CreditBalanceData;
import com.ll.backend.global.client.dto.credit.CreditChargeData;
import com.ll.backend.global.client.dto.credit.CreditChargeRequestPayload;
import com.ll.backend.global.client.dto.credit.CreditTransactionsData;
import com.ll.backend.global.client.dto.order.*;
import com.ll.backend.global.client.dto.photo.PhotoUploadData;
import com.ll.backend.global.client.dto.photo.PhotoUploadOutcome;
import com.ll.backend.global.dto.SavedPaths;
import com.ll.backend.global.storage.LocalPhotoStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriBuilder;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
@Slf4j
@RequiredArgsConstructor
public class SweetbookApiClient {
    @Value("${sweetbook.contents.template-uid}")
    private String contentsTemplateUid;
    @Value("${sweetbook.contents.break-before}")
    private String contentsBreakBefore;

    private final LocalPhotoStorage localPhotoStorage;
    private final WebClient sweetbookWebClient;
    private final ObjectMapper objectMapper;

    public SweetbookApiEnvelope<BooksListData> listBooks(
            Integer limit,
            Integer offset,
            String pdfStatusIn,
            String createdFrom,
            String createdTo) {
        try {
            SweetbookApiEnvelope<BooksListData> body = sweetbookWebClient.get()
                    .uri(uriBuilder -> queryParameters(uriBuilder, limit, offset, pdfStatusIn, createdFrom, createdTo))
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<SweetbookApiEnvelope<BooksListData>>() {
                    })
                    .block();
            log.info("Sweetbook 서버 응답 {} body={}", "listBooks", body);
            return body;
        } catch (WebClientResponseException e) {
            log.error(
                    "Sweetbook listBooks 실패 status={} 서버응답body={}",
                    e.getStatusCode().value(),
                    e.getResponseBodyAsString());
            throw e;
        }
    }

    public SweetbookApiResponse<CreateBookResponseData> createBook(CreateBookRequest request) {
        log.info("Sweetbook 요청 {} method={} path={} body={}", "createBook", "POST", "/books", request);
        try {
            WebClient.RequestHeadersSpec<?> spec = sweetbookWebClient.post()
                    .uri("/books")
                    .bodyValue(request);

            SweetbookApiResponse<CreateBookResponseData> body = spec
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<SweetbookApiResponse<CreateBookResponseData>>() {
                    })
                    .block();
            SweetbookApiResponse<CreateBookResponseData> response = requireNonNullBody(body, "createBook");
            log.info("Sweetbook 서버 응답 {} body={}", "createBook", response);
            return response;
        } catch (WebClientResponseException e) {
            log.error(
                    "Sweetbook createBook 실패 status={} 서버응답body={}",
                    e.getStatusCode().value(),
                    e.getResponseBodyAsString());
            throw e;
        }
    }

    public CreateOrderResponse createOrder(CreateOrderPayload requestBody) {
        try {
            CreateOrderResponse body = sweetbookWebClient.post()
                    .uri("/orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<CreateOrderResponse>() {
                    })
                    .block();
            CreateOrderResponse response = requireNonNullBody(body, "createOrder");
            if (!response.success()) {
                String msg = response.message() != null ? response.message() : "Sweetbook createOrder 실패";
                throw new IllegalStateException(msg);
            }
            log.info("Sweetbook 서버 응답 {} body={}", "createOrder", response);
            return response;
        } catch (WebClientResponseException e) {
            log.error(
                    "Sweetbook createOrder 실패 status={} 서버응답body={}",
                    e.getStatusCode().value(),
                    e.getResponseBodyAsString());
            throw e;
        }
    }

    public SweetbookApiResponse<CreditChargeData> chargeSandboxCredit(CreditChargeRequestPayload requestBody) {
        Objects.requireNonNull(requestBody, "requestBody");
        log.info("Sweetbook 요청 {} method={} path={} body={}", "chargeSandboxCredit", "POST", "/credits/sandbox/charge", requestBody);
        try {
            SweetbookApiResponse<CreditChargeData> body = sweetbookWebClient.post()
                    .uri("/credits/sandbox/charge")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<SweetbookApiResponse<CreditChargeData>>() {
                    })
                    .block();
            SweetbookApiResponse<CreditChargeData> response =
                    requireNonNullBody(body, "chargeSandboxCredit");
            requireSuccess(response.success(), response.message(), "chargeSandboxCredit");
            log.info("Sweetbook 서버 응답 {} body={}", "chargeSandboxCredit", response);
            return response;
        } catch (WebClientResponseException e) {
            log.error(
                    "Sweetbook chargeSandboxCredit 실패 status={} 서버응답body={}",
                    e.getStatusCode().value(),
                    e.getResponseBodyAsString());
            throw e;
        }
    }

    public SweetbookApiResponse<CreditBalanceData> getCredits() {
        log.info("Sweetbook 요청 {} method={} path={} body={}", "getCredits", "GET", "/credits", "(none)");
        try {
            SweetbookApiResponse<CreditBalanceData> body = sweetbookWebClient.get()
                    .uri("/credits")
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<SweetbookApiResponse<CreditBalanceData>>() {
                    })
                    .block();
            SweetbookApiResponse<CreditBalanceData> response =
                    requireNonNullBody(body, "getCredits");
            requireSuccess(response.success(), response.message(), "getCredits");
            log.info("Sweetbook 서버 응답 {} body={}", "getCredits", response);
            return response;
        } catch (WebClientResponseException e) {
            log.error(
                    "Sweetbook getCredits 실패 status={} 서버응답body={}",
                    e.getStatusCode().value(),
                    e.getResponseBodyAsString());
            throw e;
        }
    }

    public SweetbookApiResponse<CreditTransactionsData> getCreditTransactions(int limit, int offset) {
        int lim = Math.min(Math.max(limit, 1), 100);
        int off = Math.max(offset, 0);
        log.info(
                "Sweetbook 요청 {} method={} path={} body={}",
                "getCreditTransactions",
                "GET",
                "/credits/transactions?limit=" + lim + "&offset=" + off,
                "(none)");
        try {
            SweetbookApiResponse<CreditTransactionsData> body = sweetbookWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/credits/transactions")
                            .queryParam("limit", lim)
                            .queryParam("offset", off)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<SweetbookApiResponse<CreditTransactionsData>>() {
                    })
                    .block();
            SweetbookApiResponse<CreditTransactionsData> response =
                    requireNonNullBody(body, "getCreditTransactions");
            requireSuccess(response.success(), response.message(), "getCreditTransactions");
            log.info("Sweetbook 서버 응답 {} body={}", "getCreditTransactions", response);
            return response;
        } catch (WebClientResponseException e) {
            log.error(
                    "Sweetbook getCreditTransactions 실패 status={} 서버응답body={}",
                    e.getStatusCode().value(),
                    e.getResponseBodyAsString());
            throw e;
        }
    }

    public GetOrdersResponse getOrders(int limit, int offset) {
        int lim = Math.min(Math.max(limit, 1), 100);
        int off = Math.max(offset, 0);
        log.info(
                "Sweetbook 요청 {} method={} path={} body={}",
                "getOrders",
                "GET",
                "/orders?limit=" + lim + "&offset=" + off,
                "(none)");
        try {
            GetOrdersResponse body = sweetbookWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/orders")
                            .queryParam("limit", lim)
                            .queryParam("offset", off)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<GetOrdersResponse>() {
                    })
                    .block();
            GetOrdersResponse response = requireNonNullBody(body, "getOrders");
            requireSuccess(response.success(), response.message(), "getOrders");
            log.info("Sweetbook 서버 응답 {} body={}", "getOrders", response);
            return response;
        } catch (WebClientResponseException e) {
            log.error(
                    "Sweetbook getOrders 실패 status={} 서버응답body={}",
                    e.getStatusCode().value(),
                    e.getResponseBodyAsString());
            throw e;
        }
    }

    public GetOrderDetailResponse getOrderDetail(String orderUid) {
        Objects.requireNonNull(orderUid, "orderUid");
        String uid = orderUid.trim();
        if (uid.isEmpty()) {
            throw new IllegalArgumentException("orderUid is blank");
        }
        log.info("Sweetbook 요청 {} method={} path={} body={}", "getOrderDetail", "GET", "/orders/" + uid, "(none)");
        try {
            GetOrderDetailResponse body = sweetbookWebClient.get()
                    .uri("/orders/{orderUid}", uid)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<GetOrderDetailResponse>() {
                    })
                    .block();
            GetOrderDetailResponse response = requireNonNullBody(body, "getOrderDetail");
            requireSuccess(response.success(), response.message(), "getOrderDetail");
            log.info("Sweetbook 서버 응답 {} orderUid={} body={}", "getOrderDetail", uid, response);
            return response;
        } catch (WebClientResponseException e) {
            log.error(
                    "Sweetbook getOrderDetail 실패 orderUid={} status={} 서버응답body={}",
                    uid,
                    e.getStatusCode().value(),
                    e.getResponseBodyAsString());
            throw e;
        }
    }

    public OrderCancelResponse cancelOrder(String orderUid, String reason) {
        Objects.requireNonNull(orderUid, "orderUid");
        String uid = orderUid.trim();
        if (uid.isEmpty()) {
            throw new IllegalArgumentException("orderUid is blank");
        }
        String reasonText = reason == null ? "" : reason.trim();
        Map<String, String> cancelBody = Map.of("CancelReason", reasonText);
        log.info(
                "Sweetbook 요청 {} method={} path={} body={}",
                "cancelOrder",
                "POST",
                "/orders/" + uid + "/cancel",
                cancelBody);
        try {
            OrderCancelResponse body = sweetbookWebClient.post()
                    .uri("/orders/{orderUid}/cancel", uid)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(cancelBody)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<OrderCancelResponse>() {
                    })
                    .block();
            OrderCancelResponse response = requireNonNullBody(body, "cancelOrder");
            requireSuccess(response.success(), response.message(), "cancelOrder");
            log.info("Sweetbook 서버 응답 {} orderUid={} body={}", "cancelOrder", uid, response);
            return response;
        } catch (WebClientResponseException e) {
            log.error(
                    "Sweetbook cancelOrder 실패 orderUid={} status={} 서버응답body={}",
                    uid,
                    e.getStatusCode().value(),
                    e.getResponseBodyAsString());
            throw e;
        }
    }

    public SweetbookResponse updateOrderShipping(
            String orderUid, String recipientName, String address1) {
        Objects.requireNonNull(orderUid, "orderUid");
        String uid = orderUid.trim();
        if (uid.isEmpty()) {
            throw new IllegalArgumentException("orderUid is blank");
        }
        String name = recipientName == null ? "" : recipientName.trim();
        String addr = address1 == null ? "" : address1.trim();
        log.info(
                "Sweetbook 요청 {} method={} path={} body={}",
                "updateOrderShipping",
                "PATCH",
                "/orders/" + uid + "/shipping",
                Map.of("recipientName", name, "address1", addr));
        try {
            SweetbookResponse body = sweetbookWebClient.patch()
                    .uri("/orders/{orderUid}/shipping", uid)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of("recipientName", name, "address1", addr))
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<SweetbookResponse>() {
                    })
                    .block();
            SweetbookResponse response = requireNonNullBody(body, "updateOrderShipping");
            requireSuccess(response.success(), response.message(), "updateOrderShipping");
            log.info("Sweetbook 서버 응답 {} orderUid={} body={}", "updateOrderShipping", uid, response);
            return response;
        } catch (WebClientResponseException e) {
            log.error(
                    "Sweetbook updateOrderShipping 실패 orderUid={} status={} 서버응답body={}",
                    uid,
                    e.getStatusCode().value(),
                    e.getResponseBodyAsString());
            throw e;
        }
    }

    public SweetbookResponse deleteBook(String bookUid) {
        Objects.requireNonNull(bookUid, "bookUid");
        log.info("Sweetbook 요청 {} method={} path={} body={}", "deleteBook", "DELETE", "/books/" + bookUid, "(none)");
        try {
            SweetbookResponse body = sweetbookWebClient.delete()
                    .uri("/books/{bookUid}", bookUid)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<SweetbookResponse>() {
                    })
                    .block();
            SweetbookResponse response = requireNonNullBody(body, "deleteBook");
            log.info("Sweetbook 서버 응답 {} bookUid={} body={}", "deleteBook", bookUid, response);
            return response;
        } catch (WebClientResponseException e) {
            log.error(
                    "Sweetbook deleteBook 실패 bookUid={} status={} 서버응답body={}",
                    bookUid,
                    e.getStatusCode().value(),
                    e.getResponseBodyAsString());
            throw e;
        }
    }

    public SweetbookResponse finalizeBook(String bookUid) {
        Objects.requireNonNull(bookUid, "bookUid");
        log.info("Sweetbook 요청 {} method={} path={} body={}", "finalizeBook", "POST", "/books/" + bookUid + "/finalization", "(none)");
        try {
            SweetbookResponse body = sweetbookWebClient.post()
                    .uri("/books/{bookUid}/finalization", bookUid)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<SweetbookResponse>() {
                    })
                    .block();
            SweetbookResponse response = requireNonNullBody(body, "finalizeBook");
            log.info("Sweetbook 서버 응답 {} bookUid={} body={}", "finalizeBook", bookUid, response);
            return response;
        } catch (WebClientResponseException e) {
            log.error(
                    "Sweetbook finalizeBook 실패 bookUid={} status={} 서버응답body={}",
                    bookUid,
                    e.getStatusCode().value(),
                    e.getResponseBodyAsString());
            throw e;
        }
    }

    public SweetbookResponse estimateOrder(Map<String, Object> requestBody) {
        Objects.requireNonNull(requestBody, "requestBody");
        log.info("Sweetbook 요청 {} method={} path={} body={}", "estimateOrder", "POST", "/orders/estimate", requestBody);
        try {
            SweetbookResponse body = sweetbookWebClient.post()
                    .uri("/orders/estimate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<SweetbookResponse>() {
                    })
                    .block();
            SweetbookResponse response = requireNonNullBody(body, "estimateOrder");
            requireSuccess(response.success(), response.message(), "estimateOrder");
            log.info("Sweetbook 서버 응답 {} body={}", "estimateOrder", response);
            return response;
        } catch (WebClientResponseException e) {
            log.error(
                    "Sweetbook estimateOrder 실패 status={} 서버응답body={}",
                    e.getStatusCode().value(),
                    e.getResponseBodyAsString());
            throw e;
        }
    }

    public SweetbookResponse deleteBookPhoto(String bookUid, String fileName) {
        Objects.requireNonNull(bookUid, "bookUid");
        Objects.requireNonNull(fileName, "fileName");
        String name = fileName.trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("fileName is blank");
        }
        String ctx = "bookUid=" + bookUid + " fileName=" + name;
        log.info("Sweetbook 요청 {} method={} path={} body={}", "deleteBookPhoto", "DELETE", "/books/" + bookUid + "/photos/" + name, "(none)");
        try {
            String raw = sweetbookWebClient.delete()
                    .uri("/books/{bookUid}/photos/{fileName}", bookUid, name)
                    .retrieve()
                    .bodyToMono(String.class)
                    .defaultIfEmpty("")
                    .block();
            String display = (raw == null || raw.isBlank()) ? "(empty)" : raw;
            log.info("Sweetbook 서버 응답 {} {} body={}", "deleteBookPhoto", ctx, display);
            if (raw == null || raw.isBlank()) {
                throw new IllegalStateException("Sweetbook deleteBookPhoto 응답 본문이 비어 있습니다.");
            }
            try {
                SweetbookResponse parsed = objectMapper.readValue(raw, new TypeReference<>() {
                });
                return requireNonNullBody(parsed, "deleteBookPhoto");
            } catch (Exception e) {
                throw new IllegalStateException("Sweetbook deleteBookPhoto 응답 파싱에 실패했습니다.", e);
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
        log.info("Sweetbook 요청 {} method={} path={} body={}", "getBookPhotos", "GET", "/books/" + bookUid + "/photos", "(none)");
        try {
            SweetbookApiEnvelope<BookPhotosData> response = sweetbookWebClient.get()
                    .uri("/books/{bookUid}/photos", bookUid)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<SweetbookApiEnvelope<BookPhotosData>>() {
                    })
                    .block();
            log.info("Sweetbook 서버 응답 {} bookUid={} body={}", "getBookPhotos", bookUid, response);
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
                    .bodyToMono(new ParameterizedTypeReference<SweetbookApiEnvelope<PhotoUploadData>>() {
                    })
                    .block();
            log.info("Sweetbook 서버 응답 {} bookUid={} body={}", "uploadPhoto", bookUid, response);

            String originalPath = null;
            if (response != null && response.success() && response.data() != null) {
                SavedPaths paths = saveUploadedPhotoLocally(bookUid, bytes, filename);
                originalPath = paths.originalAbsolutePath();
            }
            return new PhotoUploadOutcome(response, originalPath);
        } catch (WebClientResponseException e) {
            log.error(
                    "Sweetbook uploadPhoto 실패 bookUid={} status={} 서버응답body={}",
                    bookUid,
                    e.getStatusCode().value(),
                    e.getResponseBodyAsString());
            throw e;
        }
    }

    public AddBookContentsResponse addBookContents(String bookUid, AddBookContentsRequest request) {
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
            AddBookContentsResponse body = sweetbookWebClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/books/{bookUid}/contents")
                            .queryParam("breakBefore", contentsBreakBefore)
                            .build(bookUid))
                    .body(BodyInserters.fromMultipartData(builder.build()))
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<AddBookContentsResponse>() {
                    })
                    .block();
            AddBookContentsResponse response = requireNonNullBody(body, "addBookContents");
            log.info("Sweetbook 서버 응답 {} bookUid={} body={}", "addBookContents", bookUid, response);
            return response;
        } catch (WebClientResponseException e) {
            log.error(
                    "Sweetbook addBookContents 실패 bookUid={} status={} 서버응답body={}",
                    bookUid,
                    e.getStatusCode().value(),
                    e.getResponseBodyAsString());
            throw e;
        }
    }

    public SweetbookResponse uploadBookCover(
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
            MultipartFile back = Objects.requireNonNull(backPhoto);
            String backName = filenameOrDefault(back.getOriginalFilename(), "back.jpg");
            byte[] backBytes;
            try {
                backBytes = back.getBytes();
            } catch (IOException e) {
                log.error("Sweetbook uploadBookCover backPhoto getBytes() 실패: {}", e.getMessage());
                throw new IllegalStateException("Failed to read backPhoto bytes", e);
            }
            MediaType backType = resolvePartMediaType(back, backName);
            builder.part("backPhoto", new ByteArrayResource(backBytes))
                    .filename(backName)
                    .contentType(backType);
        }

        try {
            SweetbookResponse body = sweetbookWebClient.post()
                    .uri("/books/{bookUid}/cover", bookUid)
                    .body(BodyInserters.fromMultipartData(builder.build()))
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<SweetbookResponse>() {
                    })
                    .block();
            SweetbookResponse response = requireNonNullBody(body, "uploadBookCover");
            log.info("Sweetbook 서버 응답 {} bookUid={} body={}", "uploadBookCover", bookUid, response);
            return response;
        } catch (WebClientResponseException e) {
            log.error(
                    "Sweetbook uploadBookCover 실패 bookUid={} status={} 서버응답body={}",
                    bookUid,
                    e.getStatusCode().value(),
                    e.getResponseBodyAsString());
            throw e;
        }
    }

    private static URI queryParameters(
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

    private static String filenameOrDefault(String original, String fallback) {
        return (original != null && !original.isBlank()) ? original : fallback;
    }

    private static <T> T requireNonNullBody(T body, String operation) {
        if (body == null) {
            throw new IllegalStateException("Sweetbook " + operation + " 응답 본문이 비어 있습니다.");
        }
        return body;
    }

    private static void requireSuccess(boolean success, String message, String operation) {
        if (!success) {
            String msg = message != null ? message : "Sweetbook " + operation + " 실패";
            throw new IllegalStateException(msg);
        }
    }

    private SavedPaths saveUploadedPhotoLocally(
            String bookUid, byte[] bytes, String originalFilename) {
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
