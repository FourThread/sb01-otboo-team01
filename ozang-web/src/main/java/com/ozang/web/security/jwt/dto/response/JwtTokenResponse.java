package com.ozang.web.security.jwt.dto.response;

public record JwtTokenResponse(
    String accessToken,
    String refreshToken
) {

}
