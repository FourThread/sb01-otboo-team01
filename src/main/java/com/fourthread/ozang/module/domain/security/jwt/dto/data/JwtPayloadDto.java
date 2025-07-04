package com.fourthread.ozang.module.domain.security.jwt.dto.data;

import com.fourthread.ozang.module.domain.user.dto.type.Role;
import java.util.UUID;

public record JwtPayloadDto(
    UUID userId,
    String email,
    String name,
    Role role
) {

}
