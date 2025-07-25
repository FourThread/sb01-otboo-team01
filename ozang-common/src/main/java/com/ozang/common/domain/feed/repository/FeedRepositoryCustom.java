package com.ozang.common.domain.feed.repository;

import com.ozang.common.domain.feed.dto.FeedDto;
import com.ozang.common.domain.feed.dto.request.FeedPaginationRequest;
import java.util.List;
import java.util.UUID;

public interface FeedRepositoryCustom {

  List<FeedDto> search(FeedPaginationRequest request, UUID likeByUserId);

  Long feedTotalCount(FeedPaginationRequest request);

}
