package com.fourthread.ozang.app.domain.security.jwt.dto.response;

public record JwtTokenResponse(
    String accessToken,
    String refreshToken
) {

}
