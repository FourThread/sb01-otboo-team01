package com.ozang.web.user.dto.request;

import com.ozang.web.user.dto.type.Gender;
import com.ozang.web.user.dto.type.Location;
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
