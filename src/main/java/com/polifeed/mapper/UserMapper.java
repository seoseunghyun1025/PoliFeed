package com.polifeed.mapper;

import com.polifeed.domain.User;
import org.apache.ibatis.annotations.Mapper;
import java.util.Optional;

@Mapper // 스프링이 "아, 이게 마이바티스 매퍼구나" 하고 인식합니다.
public interface UserMapper {

    // 1. 회원 저장 (회원가입)
    void save(User user);

    // 2. 이메일로 회원 찾기 (로그인 시 중복 확인용)
    Optional<User> findByEmail(String email);

    // 3. 소셜 ID로 회원 찾기 (이미 가입한 회원인지 확인용)
    Optional<User> findByProviderId(String providerId);
}
