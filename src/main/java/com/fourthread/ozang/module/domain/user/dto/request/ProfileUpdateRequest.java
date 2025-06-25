package com.fourthread.ozang.module.domain.user.dto.request;

import com.fourthread.ozang.module.domain.user.dto.type.Gender;
import com.fourthread.ozang.module.domain.user.dto.type.Location;
import java.time.LocalDateTime;

public record ProfileUpdateRequest(
    String name,
    Gender gender,
    LocalDateTime birthDate,
    Location location,
    Integer temperatureSensitivity
) {

}
