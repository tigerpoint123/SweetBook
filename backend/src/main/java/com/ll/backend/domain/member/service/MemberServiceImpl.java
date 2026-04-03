package com.ll.backend.domain.member.service;

import com.ll.backend.domain.member.entity.Member;
import com.ll.backend.domain.member.repository.MemberRepository;
import com.ll.backend.domain.member.vo.MemberLoginResult;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;
    private final ConcurrentHashMap<String, String> sessionIdToUsername = new ConcurrentHashMap<>();

    @Override
    public MemberLoginResult login(String username, String password) {
        if (!credentialsMatch(username, password)) {
            return new MemberLoginResult(false, "아이디 또는 비밀번호가 일치하지 않습니다.", null);
        }
        String sessionId = issueSessionId(username);
        return new MemberLoginResult(true, "로그인 성공", sessionId);
    }

    private boolean credentialsMatch(String username, String password) {
        return memberRepository.findByUsername(username)
                .map(m -> password.equals(m.getPassword()))
                .orElse(false);
    }

    private String issueSessionId(String username) {
        String sessionId = UUID.randomUUID().toString();
        sessionIdToUsername.put(sessionId, username);
        return sessionId;
    }

    @Override
    public Member getMemberInfo() {
        return null;
    }

    @Override
    public void postMember(String username, String password) {
        Member member = new Member(username, password);
        memberRepository.save(member);
    }
}
