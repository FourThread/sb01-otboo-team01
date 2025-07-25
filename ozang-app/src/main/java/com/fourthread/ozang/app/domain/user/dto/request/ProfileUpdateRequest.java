package com.fourthread.ozang.app.domain.user.dto.request;

import com.fourthread.ozang.app.domain.user.dto.type.Gender;
import com.fourthread.ozang.app.domain.user.dto.type.Location;
import java.time.LocalDate;

public record ProfileUpdateRequest(
    String name,
    Gender gender,
    LocalDate birthDate,
    Location location,
    Integer temperatureSensitivity
) {

}
