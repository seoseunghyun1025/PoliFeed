package com.polifeed.service;

import com.polifeed.domain.User;
import com.polifeed.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserMapper userMapper;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // 1. 구글에서 유저 정보 가져오기
        OAuth2User oAuth2User = super.loadUser(userRequest);

        // 2. 정보 추출 (구글 기준)
        String provider = userRequest.getClientRegistration().getRegistrationId(); // "google"
        String providerId = oAuth2User.getAttribute("sub"); // 구글의 유저 고유 ID
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");

        // 3. 우리 DB에 있는지 확인
        Optional<User> optionalUser = userMapper.findByProviderId(providerId);
        User user;

        if (optionalUser.isEmpty()) {
            // 4. 없으면 회원가입 (DB 저장)
            user = new User();
            user.setEmail(email);
            user.setUsername(name);
            user.setProvider(provider);
            user.setProviderId(providerId);
            user.setRole("ROLE_USER");
            userMapper.save(user); // 저장!
        } else {
            // 5. 있으면 정보 업데이트 (여기선 조회만)
            user = optionalUser.get();
        }

        // 6. 시큐리티 세션에 저장할 정보 리턴
        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority(user.getRole())),
                oAuth2User.getAttributes(),
                "sub" // 구글의 PK(ID)가 되는 필드명
        );
    }
}