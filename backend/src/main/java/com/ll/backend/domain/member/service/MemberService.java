package com.ll.backend.domain.member.service;

import com.ll.backend.domain.member.vo.MemberLoginResult;
import jakarta.validation.constraints.NotBlank;

import java.util.Optional;

public interface MemberService {

    MemberLoginResult login(String username, String password);

    Optional<String> getUsernameBySessionId(String sessionId);

    Optional<Long> getMemberIdBySessionId(String sessionId);

    void postMember(@NotBlank String username, @NotBlank String password);
}
