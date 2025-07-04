package com.fourthread.ozang.module.domain.security;

public record CsrfTokenResponse(
    String headerName,
    String token,
    String parameterName
) {

}
