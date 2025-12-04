package com.polifeed.domain;

import lombok.Data;
import java.time.LocalDateTime;

@Data // Lombok이 Getter, Setter 등을 자동으로 만들어줍니다.
public class User {
    private Long userId;       // DB의 user_id
    private String email;      // 이메일
    private String username;   // 이름
    private String provider;   // google, kakao
    private String providerId; // 소셜 로그인 ID
    private String role;       // 권한 (ROLE_USER)
    private LocalDateTime createdAt; // 가입일
}