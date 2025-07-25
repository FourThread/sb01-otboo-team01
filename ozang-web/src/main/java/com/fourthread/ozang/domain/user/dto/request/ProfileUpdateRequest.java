package com.fourthread.ozang.domain.user.dto.request;

import com.fourthread.ozang.module.domain.user.dto.type.Gender;
import com.fourthread.ozang.module.domain.user.dto.type.Location;
import java.time.LocalDate;

public record ProfileUpdateRequest(
    String name,
    Gender gender,
    LocalDate birthDate,
    Location location,
    Integer temperatureSensitivity
) {

}
