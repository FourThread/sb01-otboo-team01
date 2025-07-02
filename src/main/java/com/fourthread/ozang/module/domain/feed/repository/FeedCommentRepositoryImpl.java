package com.fourthread.ozang.module.domain.feed.repository;

import static com.fourthread.ozang.module.domain.feed.entity.QFeed.feed;
import static com.fourthread.ozang.module.domain.feed.entity.QFeedComment.feedComment;
import static com.fourthread.ozang.module.domain.user.entity.QUser.user;

import com.fourthread.ozang.module.domain.feed.dto.FeedCommentDto;
import com.fourthread.ozang.module.domain.feed.dto.request.CommentPaginationRequest;
import com.fourthread.ozang.module.domain.user.dto.data.UserSummary;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

@RequiredArgsConstructor
public class FeedCommentRepositoryImpl implements FeedCommentRepositoryCustom {

  private final JPAQueryFactory queryFactory;

  @Override
  public List<FeedCommentDto> searchComment(CommentPaginationRequest request) {

    return queryFactory
        .select(Projections.constructor(FeedCommentDto.class,
            feedComment.id,
            feedComment.createdAt,
            feed.id,
            Projections.constructor(UserSummary.class,
                user.id,
                user.name,
                user.profile.profileImageUrl
            ),
            feedComment.content
        ))
        .from(feedComment)
        .leftJoin(feedComment.feed, feed)
        .leftJoin(feedComment.author, user)
        .where(
            feedIdEq(request.feedId()),
            commentCursor(request.cursor(), request.idAfter())
        )
        .orderBy(feedComment.createdAt.asc())
        .limit(request.limit() + 1)
        .fetch();
  }

  @Override
  public Long commentTotalCount(CommentPaginationRequest request) {

    return queryFactory
        .select(feedComment.count())
        .from(feedComment)
        .where(
            feedIdEq(request.feedId())
        )
        .fetchOne();
  }

  private Predicate feedIdEq(UUID feedId) {
    return feed.id.eq(feedId);
  }

  private Predicate commentCursor(String cursor, UUID idAfter) {
    if (!StringUtils.hasText(cursor)) {
      return null;
    }

    LocalDateTime time = LocalDateTime.parse(cursor);

    return feedComment.createdAt.gt(time).or(
        feedComment.createdAt.eq(time)
            .and(feedComment.id.gt(idAfter))
    );
  }
}
