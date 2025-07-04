package com.fourthread.ozang.module.domain.security.jwt.dto.response;

public record JwtTokenResponse(
    String accessToken,
    String refreshToken
) {

}
