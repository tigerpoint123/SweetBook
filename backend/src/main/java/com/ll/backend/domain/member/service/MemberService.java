package com.ll.backend.domain.member.service;

import com.ll.backend.domain.member.entity.Member;
import com.ll.backend.domain.member.vo.MemberLoginResult;
import jakarta.validation.constraints.NotBlank;

public interface MemberService {

    MemberLoginResult login(String username, String password);

    Member getMemberInfo();

    void postMember(@NotBlank String username, @NotBlank String password);
}
