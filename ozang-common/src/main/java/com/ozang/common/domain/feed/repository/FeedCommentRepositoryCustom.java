package com.ozang.common.domain.feed.repository;

import com.ozang.common.domain.feed.dto.FeedCommentDto;
import com.ozang.common.domain.feed.dto.request.CommentPaginationRequest;
import java.util.List;

public interface FeedCommentRepositoryCustom {

  List<FeedCommentDto> searchComment(CommentPaginationRequest request);

  Long commentTotalCount(CommentPaginationRequest request);
}
