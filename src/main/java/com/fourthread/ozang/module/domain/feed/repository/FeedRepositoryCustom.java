package com.fourthread.ozang.module.domain.feed.repository;

import com.fourthread.ozang.module.domain.feed.dto.FeedDto;
import com.fourthread.ozang.module.domain.feed.dto.request.FeedPaginationRequest;
import java.util.List;

public interface FeedRepositoryCustom {

  List<FeedDto> search(FeedPaginationRequest request);

  Long feedTotalCount(FeedPaginationRequest request);

}
