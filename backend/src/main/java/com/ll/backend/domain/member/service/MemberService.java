package com.ll.backend.domain.member.service;

import com.ll.backend.domain.member.entity.Member;
import java.util.Optional;
import com.ll.backend.domain.member.vo.MemberLoginResult;
import jakarta.validation.constraints.NotBlank;

public interface MemberService {

    MemberLoginResult login(String username, String password);

    Optional<String> resolveUsernameBySessionId(String sessionId);

    /** 세션에 대응하는 회원 PK. 세션은 username 기반이므로 members 테이블에서 조회합니다. */
    Optional<Long> resolveMemberIdBySessionId(String sessionId);

    Member getMemberInfo();

    void postMember(@NotBlank String username, @NotBlank String password);
}
