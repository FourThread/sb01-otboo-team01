package com.ozang.common.domain.user.dto.request;

import com.ozang.common.domain.user.dto.type.Gender;
import com.ozang.common.domain.user.dto.type.Location;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ProfileUpdateRequest(
    String name,
    Gender gender,
    LocalDate birthDate,
    Location location,
    Integer temperatureSensitivity
) {

}
