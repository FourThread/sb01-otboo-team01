package com.ozang.common.domain.security.jwt.dto.response;

public record JwtTokenResponse(
    String accessToken,
    String refreshToken
) {

}
