package com.fourthread.ozang.app.domain.feed.repository;

import com.fourthread.ozang.app.domain.feed.dto.FeedCommentDto;
import com.fourthread.ozang.app.domain.feed.dto.request.CommentPaginationRequest;
import java.util.List;

public interface FeedCommentRepositoryCustom {

  List<FeedCommentDto> searchComment(CommentPaginationRequest request);

  Long commentTotalCount(CommentPaginationRequest request);
}
