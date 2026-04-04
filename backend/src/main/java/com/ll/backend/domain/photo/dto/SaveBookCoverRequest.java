package com.ll.backend.domain.photo.dto;

import jakarta.validation.constraints.NotNull;

/** subtitle·dateRange는 선택(미전달 시 빈 문자열로 저장). */
public record SaveBookCoverRequest(@NotNull Long photoId, String subtitle, String dateRange) {}
