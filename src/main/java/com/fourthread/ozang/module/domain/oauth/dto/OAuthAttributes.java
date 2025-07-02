package com.fourthread.ozang.module.domain.oauth.dto;

import com.fourthread.ozang.module.domain.user.dto.type.Role;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class OAuthAttributes {
    private String registrationId;
    private Map<String, Object> attributes;
    private String nameAttributeKey;
    private String name;
    private String email;
    private String picture;
    private Role role;

    public static OAuthAttributes of(String registrationId, String userNameAttributeName, Map<String, Object> attributes) {
        if ("naver".equals(registrationId)) {
            return ofNaver(userNameAttributeName, attributes);
        } else if ("kakao".equals(registrationId)) {
            return ofKakao(userNameAttributeName, attributes);
        }
        return ofGoogle(userNameAttributeName, attributes);
    }

    private static OAuthAttributes ofGoogle(String userNameAttributeName, Map<String, Object> attributes) {
        return OAuthAttributes.builder()
                .registrationId("google")
                .name((String) attributes.get("name"))
                .email((String) attributes.get("email"))
                .picture((String) attributes.get("picture"))
                .attributes(attributes)
                .nameAttributeKey(userNameAttributeName)
                .role(Role.USER)
                .build();
    }

    private static OAuthAttributes ofNaver(String userNameAttributeName, Map<String, Object> attributes) {
        Object responseObj = attributes.get("response");
        if (responseObj instanceof Map<?, ?> map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = (Map<String, Object>) map;

            return OAuthAttributes.builder()
                .registrationId("naver")
                .name((String) response.get("name"))
                .email((String) response.get("email"))
                .picture((String) response.get("profile_image"))
                .attributes(response)
                .nameAttributeKey(userNameAttributeName)
                .role(Role.USER)
                .build();
        }

        throw new IllegalArgumentException("Invalid response structure for Naver attributes.");
    }

    private static OAuthAttributes ofKakao(String userNameAttributeName, Map<String, Object> attributes) {
        Object accountObj = attributes.get("kakao_account");
        if (accountObj instanceof Map<?, ?> accountRaw) {
            @SuppressWarnings("unchecked")
            Map<String, Object> kakaoAccount = (Map<String, Object>) accountRaw;

            Object profileObj = kakaoAccount.get("profile");
            if (profileObj instanceof Map<?, ?> profileRaw) {
                @SuppressWarnings("unchecked")
                Map<String, Object> profile = (Map<String, Object>) profileRaw;

                return OAuthAttributes.builder()
                    .registrationId("kakao")
                    .name((String) profile.get("nickname"))
                    .email((String) kakaoAccount.get("email"))
                    .picture((String) profile.get("profile_image_url"))
                    .attributes(attributes)
                    .nameAttributeKey(userNameAttributeName)
                    .role(Role.USER)
                    .build();
            }
        }
        throw new IllegalArgumentException("Invalid response structure for Kakao attributes.");
    }
} 