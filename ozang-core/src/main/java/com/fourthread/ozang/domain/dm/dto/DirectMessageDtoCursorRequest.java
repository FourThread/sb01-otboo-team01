package com.fourthread.ozang.domain.dm.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record DirectMessageDtoCursorRequest(

    @NotNull
    UUID userId,

    String cursor,
    UUID idAfter,

    @NotNull
    Integer limit
) {

}
