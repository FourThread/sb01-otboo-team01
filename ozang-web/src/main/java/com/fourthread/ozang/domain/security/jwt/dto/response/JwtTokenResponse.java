package com.fourthread.ozang.domain.security.jwt.dto.response;

public record JwtTokenResponse(
    String accessToken,
    String refreshToken
) {

}
