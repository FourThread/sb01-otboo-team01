package com.fourthread.ozang.app.domain.user.dto.response;

import com.fourthread.ozang.app.domain.feed.entity.SortDirection;
import com.fourthread.ozang.app.domain.user.dto.data.UserDto;
import com.fourthread.ozang.app.domain.user.dto.type.SortBy;
import java.util.List;
import java.util.UUID;

public record UserCursorPageResponse(
    List<UserDto> data,
    String nextCursor,
    UUID nextIdAfter,
    boolean hasNext,
    Long totalCount,
    SortBy sortBy,
    SortDirection sortDirection
) {

}
