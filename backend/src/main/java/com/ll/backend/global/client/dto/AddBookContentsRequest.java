package com.ll.backend.global.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AddBookContentsRequest(
        String templateUid,
        @NotNull @Valid ContentsParameters parameters) {

    public AddBookContentsRequest {
        templateUid = templateUid != null ? templateUid.trim() : "";
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ContentsParameters(
            /** 템플릿 1vuzMfUnCkXS 필수. 비우면 서버가 Asia/Seoul 기준 {@code yyyy-MM}. */
            String monthYearLabel,
            /** Sweetbook에 등록된 사진의 {@code fileName} 목록(저장 DB의 sweetbookFileName). */
            @NotEmpty List<@NotBlank String> photos) {

        public ContentsParameters {
            monthYearLabel = monthYearLabel != null ? monthYearLabel.trim() : "";
        }
    }
}
