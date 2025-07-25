package com.ozang.web.user.dto.data;

import com.ozang.web.user.dto.type.Items;
import com.ozang.web.user.dto.type.Role;
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
