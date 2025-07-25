package com.fourthread.ozang.domain.user.dto.data;

import com.fourthread.ozang.module.domain.user.dto.type.Gender;
import java.time.LocalDate;
import java.util.UUID;

public record ProfileDto(
    UUID userId,
    String name,
    Gender gender,
    LocalDate birthDate,
    LocationDto location,
    Integer temperatureSensitivity,
    String profileImageUrl
) {

}
