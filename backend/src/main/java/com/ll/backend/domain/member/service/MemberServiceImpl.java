package com.ll.backend.domain.member.service;

import com.ll.backend.domain.member.entity.Member;
import com.ll.backend.domain.member.repository.MemberRepository;
import com.ll.backend.domain.member.vo.MemberLoginResult;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final ConcurrentHashMap<String, String> sessionIdToUsername = new ConcurrentHashMap<>();

    @Override
    public MemberLoginResult login(String username, String password) {
        if (!credentialsMatch(username, password)) {
            return new MemberLoginResult(false, "아이디 또는 비밀번호가 일치하지 않습니다.", null);
        }
        String sessionId = issueSessionId(username);
        return new MemberLoginResult(true, "로그인 성공", sessionId);
    }

    @Override
    public Optional<String> getUsernameBySessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(sessionIdToUsername.get(sessionId));
    }

    @Override
    public Optional<Long> getMemberIdBySessionId(String sessionId) {
        return getUsernameBySessionId(sessionId)
                .flatMap(memberRepository::findByUsername)
                .map(Member::getId);
    }

    @Override
    public void postMember(String username, String password) {
        String encoded = passwordEncoder.encode(password);
        Member member = new Member(username, encoded);
        memberRepository.save(member);
    }

    private boolean credentialsMatch(String username, String rawPassword) {
        return memberRepository
                .findByUsername(username)
                .map(m -> storedPasswordMatches(rawPassword, m.getPassword()))
                .orElse(false);
    }

    private boolean storedPasswordMatches(String rawPassword, String stored) {
        if (rawPassword == null || stored == null) {
            return false;
        }
        if (looksLikeBcryptHash(stored)) {
            return passwordEncoder.matches(rawPassword, stored);
        }
        // DB에 남아 있는 구 평문 비밀번호(기존 개발 데이터) 호환
        return rawPassword.equals(stored);
    }

    private static boolean looksLikeBcryptHash(String stored) {
        return stored.length() >= 7
                && stored.charAt(0) == '$'
                && (stored.startsWith("$2a$")
                || stored.startsWith("$2b$")
                || stored.startsWith("$2y$"));
    }

    private String issueSessionId(String username) {
        String sessionId = UUID.randomUUID().toString();
        sessionIdToUsername.put(sessionId, username);
        return sessionId;
    }
}
