package com.fourthread.ozang.module.domain.user.dto.request;

import com.fourthread.ozang.module.domain.user.dto.type.Role;

public record UserRoleUpdateRequest(
    Role role
) {

}
