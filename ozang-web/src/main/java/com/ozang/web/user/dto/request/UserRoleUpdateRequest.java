package com.ozang.web.user.dto.request;

import com.ozang.web.user.dto.type.Role;

public record UserRoleUpdateRequest(
    Role role
) {

}
