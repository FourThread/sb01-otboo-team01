package com.fourthread.ozang.module.domain.security.jwt;

import com.fourthread.ozang.module.domain.user.dto.data.UserDto;
import java.time.LocalDateTime;

public record JwtDto(
    LocalDateTime iat,
    LocalDateTime exp,
    UserDto userDto,
    String token
) {

}
