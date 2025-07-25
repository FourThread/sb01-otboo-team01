package com.ozang.web.user.dto.data;

import com.ozang.web.user.dto.type.Gender;
import com.ozang.web.user.dto.type.Location;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
