package com.ll.backend.domain.member.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Table(name = "members")
@Getter
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    /** BCrypt 해시(약 60자). 기존 평문 행은 로그인 시 마이그레이션 전까지 평문 비교로 호환. */
    @Column(nullable = false, length = 255)
    private String password;

    protected Member() {
    }

    public Member(String username, String password) {
        this.username = username;
        this.password = password;
    }

}
