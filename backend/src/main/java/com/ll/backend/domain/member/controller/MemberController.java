package com.ll.backend.domain.member.controller;

import com.ll.backend.domain.member.service.MemberService;
import com.ll.backend.domain.member.vo.LoginResponse;
import com.ll.backend.domain.member.vo.MemberInfo;
import com.ll.backend.domain.member.vo.MemberLoginResult;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Optional;

@RestController
@RequestMapping("/api/member")
@RequiredArgsConstructor
public class MemberController {

    private static final String SESSION_COOKIE_NAME = "SESSION";

    private final MemberService memberService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @NotBlank @RequestParam String username,
            @NotBlank @RequestParam String password
    ) {
        MemberLoginResult result = memberService.login(username, password);
        if (!result.success()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new LoginResponse(false, result.message(), null));
        }

        ResponseCookie cookie = ResponseCookie.from(SESSION_COOKIE_NAME, result.sessionId())
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(Duration.ofDays(7))
                .sameSite("Lax")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new LoginResponse(true, result.message(), null));
    }

    @GetMapping("/member")
    public ResponseEntity<MemberInfo> getInfo(
            @CookieValue(name = SESSION_COOKIE_NAME, required = false) String sessionId
    ) {
        if (sessionId == null || sessionId.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Optional<String> usernameOpt = memberService.getUsernameBySessionId(sessionId);
        return usernameOpt
                .map(username -> ResponseEntity.ok(new MemberInfo(username)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    @PostMapping("/member")
    public ResponseEntity<MemberInfo> register(
            @NotBlank @RequestParam String username,
            @NotBlank @RequestParam String password
    ) {
        memberService.postMember(username, password);

        return ResponseEntity.ok(new MemberInfo(username));
    }

}
