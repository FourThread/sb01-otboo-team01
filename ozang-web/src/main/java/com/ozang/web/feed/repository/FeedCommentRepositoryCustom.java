package com.ozang.web.feed.repository;

import com.ozang.web.feed.dto.FeedCommentDto;
import com.ozang.web.feed.dto.request.CommentPaginationRequest;
import java.util.List;

public interface FeedCommentRepositoryCustom {

  List<FeedCommentDto> searchComment(CommentPaginationRequest request);

  Long commentTotalCount(CommentPaginationRequest request);
}
