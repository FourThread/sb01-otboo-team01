package com.fourthread.ozang.module.domain.user.repository.custom;

import com.fourthread.ozang.module.domain.feed.entity.SortDirection;
import com.fourthread.ozang.module.domain.user.dto.data.UserDto;
import com.fourthread.ozang.module.domain.user.dto.response.UserCursorPageResponse;
import com.fourthread.ozang.module.domain.user.dto.type.Role;
import com.fourthread.ozang.module.domain.user.dto.type.SortBy;
import com.fourthread.ozang.module.domain.user.entity.QProfile;
import com.fourthread.ozang.module.domain.user.entity.QUser;
import com.fourthread.ozang.module.domain.user.entity.User;
import com.fourthread.ozang.module.domain.user.mapper.UserMapper;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UserCustomRepositoryImpl implements UserCustomRepository {

  private final JPAQueryFactory queryFactory;
  private final UserMapper userMapper;

  @Override
  public UserCursorPageResponse searchUsers(
      String cursor,
      UUID idAfter,
      int limit,
      SortDirection sortDirection,
      String emailLike,
      Role roleEqual,
      Boolean locked
  ) {
    QUser user = QUser.user;
    QProfile profile = QProfile.profile;

    BooleanBuilder builder = new BooleanBuilder();
    if (emailLike != null && !emailLike.isBlank()) {
      builder.and(user.email.containsIgnoreCase(emailLike));
    }
    if (roleEqual != null) {
      builder.and(user.role.eq(roleEqual));
    }
    if (locked != null) {
      builder.and(user.locked.eq(locked));
    }

    if (cursor != null && idAfter != null) {
      builder.and(applyCursorCondition(user, cursor, idAfter, sortDirection));
    }

    Order order = sortDirection == SortDirection.ASCENDING ? Order.ASC : Order.DESC;
    List<OrderSpecifier<?>> orderSpecifiers = List.of(
        new OrderSpecifier<>(order, user.email),
        new OrderSpecifier<>(order, user.id) // Tie-breaker
    );

    List<User> users = queryFactory
        .selectFrom(user).distinct()
        .leftJoin(user.profile, profile).fetchJoin()
        .where(builder)
        .orderBy(orderSpecifiers.toArray(new OrderSpecifier[0]))
        .limit(limit + 1)
        .fetch();

    boolean hasNext = users.size() > limit;
    if (hasNext) {
      users = users.subList(0, limit);
    }

    List<UserDto> content = users.stream()
        .map(userMapper::toDto)
        .toList();

    String nextCursor = null;
    UUID nextIdAfter = null;
    if (hasNext && !users.isEmpty()) {
      User last = users.get(users.size() - 1);
      nextCursor = last.getEmail();
      nextIdAfter = last.getId();
    }

    Long totalCount = queryFactory
        .select(user.count())
        .from(user)
        .where(builder)
        .fetchOne();

    return new UserCursorPageResponse(
        content,
        nextCursor,
        nextIdAfter,
        hasNext,
        totalCount,
        SortBy.EMAIL,
        sortDirection
    );
  }

  private BooleanExpression applyCursorCondition(QUser user, String cursor, UUID idAfter, SortDirection direction) {
    if (direction == SortDirection.ASCENDING) {
      return user.email.gt(cursor)
          .or(user.email.eq(cursor).and(user.id.gt(idAfter)));
    } else {
      return user.email.lt(cursor)
          .or(user.email.eq(cursor).and(user.id.lt(idAfter)));
    }
  }
}

