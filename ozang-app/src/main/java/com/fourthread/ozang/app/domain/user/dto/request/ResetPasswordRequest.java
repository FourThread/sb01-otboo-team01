package com.fourthread.ozang.app.domain.user.dto.request;

import jakarta.validation.constraints.Email;

public record ResetPasswordRequest(
    @Email
    String email
) {

}
