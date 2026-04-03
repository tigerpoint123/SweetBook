package com.ll.backend.domain.member.vo;

public record LoginResponse(
        boolean success,
        String message,
        String token
) {
}
