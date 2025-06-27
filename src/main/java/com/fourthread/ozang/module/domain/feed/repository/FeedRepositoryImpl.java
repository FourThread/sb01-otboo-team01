package com.fourthread.ozang.module.domain.feed.repository;

import static com.fourthread.ozang.module.domain.feed.entity.QFeed.feed;
import static com.fourthread.ozang.module.domain.user.entity.QUser.user;
import static com.fourthread.ozang.module.domain.weather.entity.QWeather.weather;
import static com.querydsl.core.group.GroupBy.groupBy;
import static com.querydsl.core.types.dsl.Expressions.numberTemplate;

import com.fourthread.ozang.module.domain.feed.dto.FeedDto;
import com.fourthread.ozang.module.domain.feed.dto.dummy.OotdDto;
import com.fourthread.ozang.module.domain.feed.dto.dummy.SortDirection;
import com.fourthread.ozang.module.domain.feed.dto.request.FeedPaginationRequest;
import com.fourthread.ozang.module.domain.user.dto.data.UserSummary;
import com.fourthread.ozang.module.domain.weather.dto.PrecipitationDto;
import com.fourthread.ozang.module.domain.weather.dto.TemperatureDto;
import com.fourthread.ozang.module.domain.weather.dto.WeatherSummaryDto;
import com.fourthread.ozang.module.domain.weather.dto.type.PrecipitationType;
import com.fourthread.ozang.module.domain.weather.dto.type.SkyStatus;
import com.querydsl.core.types.ConstructorExpression;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.QList;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FeedRepositoryImpl implements FeedRepositoryCustom {

  private final static String ORDER_BY_CREATED_AT = "createdAt";

  private final JPAQueryFactory queryFactory;

  @Override
  public List<FeedDto> search(FeedPaginationRequest request) {

    return queryFactory
        .from(feed)
        .leftJoin(feed.author, user).fetchJoin()
        .leftJoin(feed.weather, weather).fetchJoin()
        .where(
            keywordLike(request.keywordLike()),
            skyStatusEqual(request.skyStatusEqual()),
            precipitationTypeEqual(request.precipitationTypeEqual()),
            authorIdEqual(request.authorIdEqual()),
            cursor(request.cursor(), request.idAfter())
        )
        .orderBy(order(request.sortBy(), request.sortDirection()))
        .limit(pagingLimit(request.limit()) + 1)
        .transform(groupBy(feed.id)
            .list(getFeedDto()));
  }

  @Override
  public Long feedTotalCount(FeedPaginationRequest request) {

    return queryFactory
        .select(feed.count())
        .from(feed)
        .leftJoin(feed.author, user).fetchJoin()
        .leftJoin(feed.weather, weather).fetchJoin()
        .where(
            keywordLike(request.keywordLike()),
            skyStatusEqual(request.skyStatusEqual()),
            precipitationTypeEqual(request.precipitationTypeEqual()),
            authorIdEqual(request.authorIdEqual())
        )
        .fetchOne();
  }

  private Predicate cursor(String cursor, String idAfter) {
    if (cursor == null) {
      return null;
    }

    Instant instant = Instant.parse(cursor);
    LocalDateTime time = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    UUID preFeedId = UUID.fromString(idAfter);

    return feed.createdAt.lt(time).or(
        feed.createdAt.eq(time)
            .and(feed.id.lt(preFeedId))
    );
  }

  private Predicate authorIdEqual(UUID authorId) {
    return user.id.eq(authorId);
  }

  private Predicate precipitationTypeEqual(PrecipitationType precipitationType) {
    return weather.precipitation.type.eq(precipitationType);
  }

  private Predicate skyStatusEqual(SkyStatus skyStatus) {
    return weather.skyStatus.eq(skyStatus);
  }

  private BooleanExpression keywordLike(String keywordLike) {
    String keyword = "%" + keywordLike + "%";

    return feed.content.likeIgnoreCase(keyword);
  }

  private ConstructorExpression<FeedDto> getFeedDto() {
    return Projections.constructor(FeedDto.class,
        feed.id,
        feed.createdAt,
        feed.updatedAt,
        getUserSummary(),
        getWeatherSummaryDto(),
        getOotdDto(),
        feed.content,
        feed.likeCount,
        feed.commentCount,
        Expressions.constant(true)
    );
  }

  private long pagingLimit(@NotNull Integer limit) {
    return (limit == 0) ? 20L : limit;
  }

  private QList getOotdDto() {
    return Projections.list(
        Projections.constructor(OotdDto.class)
    );
  }

  private ConstructorExpression<WeatherSummaryDto> getWeatherSummaryDto() {
    return Projections.constructor(WeatherSummaryDto.class,
        weather.id,
        weather.skyStatus,
        getPrecipitationDto(),
        getTemperatureDto()
    );
  }

  private ConstructorExpression<TemperatureDto> getTemperatureDto() {
    return Projections.constructor(TemperatureDto.class,
        weather.temperature.current,
        weather.temperature.comparedToDayBefore,
        weather.temperature.min,
        weather.temperature.max
    );
  }

  private ConstructorExpression<PrecipitationDto> getPrecipitationDto() {
    return Projections.constructor(PrecipitationDto.class,
        weather.precipitation.type,
        weather.precipitation.amount,
        weather.precipitation.probability
    );
  }

  private ConstructorExpression<UserSummary> getUserSummary() {
    return Projections.constructor(UserSummary.class,
        user.id,
        user.name,
        user.profile.profileImageUrl
    );
  }

  private OrderSpecifier<?> order(
      @NotNull String sortBy,
      @NotNull SortDirection sortDirection
  ) {

    boolean sortDirectionAsc = isSortDirectionAsc(sortDirection);
    boolean sortByCreatedAt = isSortByCreatedAt(sortBy);

    if (sortByCreatedAt) {
      if (sortDirectionAsc) {
        return feed.createdAt.asc();
      } else {
        return feed.createdAt.desc();
      }
    } else {
      if (sortDirectionAsc) {
        return numberTemplate(Integer.class, "{0}", feed.likeCount).asc();
      } else {
        return numberTemplate(Integer.class, "{0}", feed.likeCount).desc();
      }
    }
  }

  private boolean isSortDirectionAsc(SortDirection sortDirection) {
    return sortDirection == SortDirection.ASCENDING;
  }

  private boolean isSortByCreatedAt(String sortBy) {
    return sortBy.equals(ORDER_BY_CREATED_AT);
  }
}
