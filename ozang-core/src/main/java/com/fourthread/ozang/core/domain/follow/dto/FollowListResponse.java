package com.fourthread.ozang.core.domain.follow.dto;

import com.fourthread.ozang.core.domain.follow.dto.FollowDto;
import java.util.List;
import java.util.UUID;

public record FollowListResponse(
        List<FollowDto> data,
        String nextCursor,
        UUID nextIdAfter,
        boolean hasNext,
        int totalCount,
        String sortBy,
        String sortDirection
) {

}
