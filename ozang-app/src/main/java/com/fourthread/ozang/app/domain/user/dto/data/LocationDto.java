package com.fourthread.ozang.app.domain.user.dto.data;

import java.util.List;

public record LocationDto(
    Double latitude,
    Double longitude,
    Integer x,
    Integer y,
    List<String> locationNames
) {

}
