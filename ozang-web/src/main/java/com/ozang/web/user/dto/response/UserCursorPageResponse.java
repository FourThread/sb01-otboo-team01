package com.ozang.web.user.dto.response;

import com.ozang.web.feed.entity.SortDirection;
import com.ozang.web.user.dto.data.UserDto;
import com.ozang.web.user.dto.type.SortBy;
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
