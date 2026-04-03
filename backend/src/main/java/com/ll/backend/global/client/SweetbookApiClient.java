package com.ll.backend.global.client;

import com.ll.backend.global.client.dto.PhotoUploadData;
import com.ll.backend.global.client.dto.SweetbookApiEnvelope;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class SweetbookApiClient {
    private final WebClient sweetbookWebClient;

    public SweetbookApiClient(WebClient sweetbookWebClient) {
        this.sweetbookWebClient = sweetbookWebClient;
    }

    public Map<String, Object> getBooks() {
        return sweetbookWebClient.get()
                .uri("/books")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                })
                .block();
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
            builder.part("files", file.getResource());
        }
        return sweetbookWebClient.post()
                .uri("/books/{bookUid}/photos", bookUid)
                .body(BodyInserters.fromMultipartData(builder.build()))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<SweetbookApiEnvelope<PhotoUploadData>>() {
                })
                .block();
    }
}
