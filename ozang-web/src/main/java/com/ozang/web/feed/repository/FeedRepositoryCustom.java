package com.ozang.web.feed.repository;

import com.ozang.web.feed.dto.FeedDto;
import com.ozang.web.feed.dto.request.FeedPaginationRequest;
import java.util.List;
import java.util.UUID;

public interface FeedRepositoryCustom {

  List<FeedDto> search(FeedPaginationRequest request, UUID likeByUserId);

  Long feedTotalCount(FeedPaginationRequest request);

}
