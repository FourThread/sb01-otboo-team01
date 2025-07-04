package com.fourthread.ozang.module.domain.user.dto.data;

import com.fourthread.ozang.module.domain.security.jwt.dto.data.JwtPayloadDto;
import com.fourthread.ozang.module.domain.user.dto.type.Items;
import com.fourthread.ozang.module.domain.user.dto.type.Role;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record UserDto(
    UUID id,
    LocalDateTime createdAt,
    String email,
    String name,
    Role role,
    List<Items> linkedOAuthProviders,
    Boolean locked
) {

}
