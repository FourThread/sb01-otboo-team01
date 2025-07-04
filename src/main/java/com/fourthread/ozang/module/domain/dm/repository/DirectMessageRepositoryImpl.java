package com.fourthread.ozang.module.domain.dm.repository;

import static com.fourthread.ozang.module.domain.dm.entity.QDirectMessage.directMessage;

import com.fourthread.ozang.module.domain.dm.dto.DirectMessageDtoCursorRequest;
import com.fourthread.ozang.module.domain.dm.dto.DmItems;
import com.fourthread.ozang.module.domain.user.dto.data.UserSummary;
import com.fourthread.ozang.module.domain.user.entity.QUser;
import com.querydsl.core.types.ConstructorExpression;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

@RequiredArgsConstructor
public class DirectMessageRepositoryImpl implements DirectMessageRepositoryCustom {

  private final JPAQueryFactory queryFactory;

  @Override
  public List<DmItems> retrieveDm(DirectMessageDtoCursorRequest request) {

    QUser sender = new QUser("sender");
    QUser receiver = new QUser("receiver");

    return queryFactory
        .select(getDmItems(sender, receiver))
        .from(directMessage)
        .leftJoin(directMessage.sender, sender)
        .leftJoin(sender.profile)
        .leftJoin(directMessage.receiver, receiver)
        .leftJoin(receiver.profile)
        .where(
            userIdEq(request.userId()),
            cursor(request.cursor(), request.idAfter())
        )
        .orderBy(directMessage.createdAt.asc())
        .limit(request.limit() + 1)
        .fetch();
  }

  @Override
  public Long count(DirectMessageDtoCursorRequest request) {

    QUser sender = new QUser("sender");
    QUser receiver = new QUser("receiver");

    return queryFactory
        .select(directMessage.count())
        .from(directMessage)
        .leftJoin(directMessage.sender, sender)
        .leftJoin(sender.profile)
        .leftJoin(directMessage.receiver, receiver)
        .leftJoin(receiver.profile)
        .where(
            userIdEq(request.userId())
        )
        .fetchOne();
  }

  private Predicate cursor(String requestCursor, UUID requestAfterId) {

    if (!StringUtils.hasText(requestCursor) || requestAfterId == null) {
      return null;
    }

    LocalDateTime cursor = LocalDateTime.parse(requestCursor);

    return directMessage.createdAt.gt(cursor)
        .or(directMessage.createdAt.eq(cursor)
                .and(directMessage.id.gt(requestAfterId)));
  }

  private Predicate userIdEq(@NotNull UUID userId) {

    return directMessage.sender.id.eq(userId)
        .or(directMessage.receiver.id.eq(userId));
  }

  private ConstructorExpression<DmItems> getDmItems(QUser sender, QUser receiver) {
    return Projections.constructor(DmItems.class,
        directMessage.id,
        directMessage.createdAt,
        getUserSummary(sender),
        getUserSummary(receiver),
        directMessage.content
    );
  }

  private ConstructorExpression<UserSummary> getUserSummary(QUser user) {
    return Projections.constructor(UserSummary.class,
        user.id,
        user.name,
        user.profile.profileImageUrl
    );
  }
}
