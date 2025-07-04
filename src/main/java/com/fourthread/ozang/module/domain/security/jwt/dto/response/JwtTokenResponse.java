package com.fourthread.ozang.module.domain.security.jwt.dto.response;

import java.time.Instant;

public record JwtTokenResponse(
    String accessToken,
    String refreshToken,
    Instant accessTokenExpiresIn ,
    Instant refreshTokenExpiresIn
) {

}
