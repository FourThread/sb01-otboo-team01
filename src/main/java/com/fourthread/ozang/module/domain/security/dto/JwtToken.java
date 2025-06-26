package com.fourthread.ozang.module.domain.security.dto;

import lombok.Builder;

@Builder
public record JwtToken(
    String grantType,
    String accessToken,
    String refreshToken
) {

}
