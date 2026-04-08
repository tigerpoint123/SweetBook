package com.ll.backend.global.client.dto.book;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AddBookContentsRequest(
        String templateUid,
        @NotNull @Valid ContentsParameters parameters
) {
    public AddBookContentsRequest {
        templateUid = templateUid != null ? templateUid.trim() : "";
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ContentsParameters(
            String monthYearLabel,
            @NotEmpty List<@NotBlank String> photos) {

        public ContentsParameters {
            monthYearLabel = monthYearLabel != null ? monthYearLabel.trim() : "";
        }
    }
}
