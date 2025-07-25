package com.ozang.common.domain.security.oauth.dto;

import com.ozang.common.domain.user.dto.type.Role;
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
} 