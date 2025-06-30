package com.fourthread.ozang.module.domain.feed.repository;

import com.fourthread.ozang.module.domain.feed.dto.FeedCommentDto;
import com.fourthread.ozang.module.domain.feed.dto.request.CommentPaginationRequest;
import java.util.List;

public interface FeedCommentRepositoryCustom {

  List<FeedCommentDto> searchComment(CommentPaginationRequest request);

  Long commentTotalCount(CommentPaginationRequest request);
}
