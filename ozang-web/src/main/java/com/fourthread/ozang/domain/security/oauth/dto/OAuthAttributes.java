package com.fourthread.ozang.domain.security.oauth.dto;

import com.fourthread.ozang.domain.user.dto.type.Role;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;

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