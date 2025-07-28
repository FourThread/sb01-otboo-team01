package com.fourthread.ozang.app.domain.user.dto.request;

import com.fourthread.ozang.core.domain.user.dto.type.Role;

public record UserRoleUpdateRequest(
    Role role
) {

}
