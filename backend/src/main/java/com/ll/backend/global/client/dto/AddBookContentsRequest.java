package com.ll.backend.global.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AddBookContentsRequest(
        @NotEmpty List<@NotBlank String> rowPhotos,
        /**
         * Sweetbook 템플릿(예: 1vuzMfUnCkXS) 필수 파라미터 {@code monthYearLabel}. 비우면 서버가 현재(Asia/Seoul) 기준 {@code yyyy-MM} 사용.
         */
        String monthYearLabel) {

    public AddBookContentsRequest {
        monthYearLabel = monthYearLabel != null ? monthYearLabel.trim() : "";
    }
}
