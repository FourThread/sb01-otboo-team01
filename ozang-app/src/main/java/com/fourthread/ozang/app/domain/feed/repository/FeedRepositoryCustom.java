package com.fourthread.ozang.app.domain.feed.repository;

import com.fourthread.ozang.app.domain.feed.dto.FeedDto;
import com.fourthread.ozang.app.domain.feed.dto.request.FeedPaginationRequest;
import java.util.List;
import java.util.UUID;

public interface FeedRepositoryCustom {

  List<FeedDto> search(FeedPaginationRequest request, UUID likeByUserId);

  Long feedTotalCount(FeedPaginationRequest request);

}
