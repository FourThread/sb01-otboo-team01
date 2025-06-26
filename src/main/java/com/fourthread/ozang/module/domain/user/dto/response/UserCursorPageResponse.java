package com.fourthread.ozang.module.domain.user.dto.response;

import com.fourthread.ozang.module.domain.feed.dto.dummy.SortDirection;
import com.fourthread.ozang.module.domain.user.dto.data.UserDto;
import java.util.List;
import java.util.UUID;

public record UserCursorPageResponse(
    List<UserDto> content,
    String nextCursor,
    UUID nextIdAfter,
    boolean hasNext,
    Long totalCount,
    String sortBy,
    SortDirection sortDirection
) {

}
