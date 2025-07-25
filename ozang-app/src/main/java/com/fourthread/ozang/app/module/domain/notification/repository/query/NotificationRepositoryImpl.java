package com.fourthread.ozang.module.domain.notification.repository.query;

import com.fourthread.ozang.module.domain.clothes.dto.response.SortDirection;
import com.fourthread.ozang.module.domain.notification.entity.Notification;
import com.fourthread.ozang.module.domain.notification.entity.QNotification;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class NotificationRepositoryImpl implements NotificationRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    QNotification notification = QNotification.notification;

    @Override
    public List<Notification> findAllByCondition(UUID receiverId,
                                                 String cursor,
                                                 UUID idAfter,
                                                 int limit,
                                                 SortDirection direction) {

        return queryFactory
                .selectFrom(notification)
                .where(
                        notification.receiverId.eq(receiverId),
                        cursorCondition(cursor, idAfter, direction)
                )
                .orderBy(getOrderSpecifiers(direction))
                .limit(limit)
                .fetch();
    }

    private BooleanExpression cursorCondition(String cursor, UUID idAfter, SortDirection direction) {
        if (cursor == null) return null;

        LocalDateTime cursorTime = LocalDateTime.parse(cursor);

        BooleanExpression timeCondition;
        BooleanExpression idCondition;

        if (direction.isAsc()) {
            timeCondition = notification.createdAt.gt(cursorTime);
            idCondition = notification.createdAt.eq(cursorTime)
                    .and(idAfter != null ? notification.id.gt(idAfter) : null);
        } else {
            timeCondition = notification.createdAt.lt(cursorTime);
            idCondition = notification.createdAt.eq(cursorTime)
                    .and(idAfter != null ? notification.id.lt(idAfter) : null);
        }

        return timeCondition.or(idCondition);
    }

    private OrderSpecifier<?>[] getOrderSpecifiers(SortDirection direction) {
        return direction.isAsc()
                ? new OrderSpecifier[] {
                notification.createdAt.asc().nullsLast(),
                notification.id.asc()
        }
                : new OrderSpecifier[] {
                notification.createdAt.desc().nullsLast(),
                notification.id.desc()
        };
    }

    @Override
    public int countByReceiverId(UUID receiverId) {
        return Math.toIntExact(queryFactory
                .select(notification.count())
                .from(notification)
                .where(notification.receiverId.eq(receiverId))
                .fetchOne());
    }
}