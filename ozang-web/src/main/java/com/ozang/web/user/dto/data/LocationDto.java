package com.ozang.web.user.dto.data;

import java.util.List;

public record LocationDto(
    Double latitude,
    Double longitude,
    Integer x,
    Integer y,
    List<String> locationNames
) {

}
