package com.ll.backend.domain.sweetbook.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FinalizeBookRequest(@NotNull @Min(0) Long price) {}
