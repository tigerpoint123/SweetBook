package com.ll.backend.domain.member.vo;

public record MemberLoginResult(boolean success, String message, String sessionId) {
}
