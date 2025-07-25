package com.fourthread.ozang.module.domain.feed.repository;

import static com.fourthread.ozang.module.domain.clothes.entity.QClothes.clothes;
import static com.fourthread.ozang.module.domain.clothes.entity.QClothesAttribute.clothesAttribute;
import static com.fourthread.ozang.module.domain.clothes.entity.QClothesAttributeDefinition.clothesAttributeDefinition;
import static com.fourthread.ozang.module.domain.feed.entity.QFeed.feed;
import static com.fourthread.ozang.module.domain.feed.entity.QFeedClothes.feedClothes;
import static com.fourthread.ozang.module.domain.feed.entity.QFeedLike.feedLike;
import static com.fourthread.ozang.module.domain.user.entity.QUser.user;
import static com.fourthread.ozang.module.domain.weather.entity.QWeather.weather;
import static com.querydsl.core.group.GroupBy.groupBy;
import static com.querydsl.core.group.GroupBy.list;
import static com.querydsl.core.types.dsl.Expressions.numberTemplate;

import com.fourthread.ozang.module.domain.clothes.dto.response.ClothesAttributeWithDefDto;
import com.fourthread.ozang.module.domain.clothes.dto.response.OotdDto;
import com.fourthread.ozang.module.domain.feed.dto.FeedDto;
import com.fourthread.ozang.module.domain.feed.dto.request.FeedPaginationRequest;
import com.fourthread.ozang.module.domain.feed.entity.SortBy;
import com.fourthread.ozang.module.domain.feed.entity.SortDirection;
import com.fourthread.ozang.module.domain.user.dto.data.UserSummary;
import com.fourthread.ozang.module.domain.weather.dto.PrecipitationDto;
import com.fourthread.ozang.module.domain.weather.dto.TemperatureDto;
import com.fourthread.ozang.module.domain.weather.dto.WeatherSummaryDto;
import com.fourthread.ozang.module.domain.weather.dto.type.PrecipitationType;
import com.fourthread.ozang.module.domain.weather.dto.type.SkyStatus;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ConstructorExpression;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

@RequiredArgsConstructor
public class FeedRepositoryImpl implements FeedRepositoryCustom {

  private final JPAQueryFactory queryFactory;

  @Override
  public List<FeedDto> search(FeedPaginationRequest request, UUID likeByUserId) {
    Map<UUID, List<OotdDto>> clothesMap = new HashMap<>();
    Map<UUID, List<ClothesAttributeWithDefDto>> attributeMap = new HashMap<>();

    List<FeedDto> feeds = getFeeds(request, likeByUserId);

    List<UUID> feedIds = feeds.stream()
        .map(FeedDto::id)
        .toList();
    if (feedIds.isEmpty()) {
      return feeds;
    }

    List<Tuple> clothesData = getClothesData(feedIds);
    List<UUID> clothesIds = getClothesIds(clothesData);

    attributeMap = queryClothesAttributes(clothesIds, attributeMap);

    Map<UUID, List<ClothesAttributeWithDefDto>> clothesWithDefMap = attributeMap;
    addOotd(clothesData, clothesWithDefMap, clothesMap);

    return convertToFeedDto(feeds, clothesMap);

  }

  private List<FeedDto> convertToFeedDto(List<FeedDto> feeds, Map<UUID, List<OotdDto>> clothesMap) {
    return feeds.stream()
        .map(feedDto -> FeedDto.builder()
            .id(feedDto.id())
            .createdAt(feedDto.createdAt())
            .updatedAt(feedDto.updatedAt())
            .author(feedDto.author())
            .weather(feedDto.weather())
            .ootds(clothesMap.getOrDefault(feedDto.id(), new ArrayList<>()))
            .content(feedDto.content())
            .likeCount(feedDto.likeCount())
            .commentCount(feedDto.commentCount())
            .likedByMe(feedDto.likedByMe())
            .build())
        .toList();
  }

  private void addOotd(List<Tuple> clothesData,
      Map<UUID, List<ClothesAttributeWithDefDto>> clothesWithDefMap,
      Map<UUID, List<OotdDto>> clothesMap) {
    clothesData.forEach(tuple -> {

      UUID clothesId = tuple.get(clothes.id);

      OotdDto ootdDto = new OotdDto(
          clothesId,
          tuple.get(clothes.name),
          tuple.get(clothes.imageUrl),
          tuple.get(clothes.type),
          clothesWithDefMap.getOrDefault(clothesId, new ArrayList<>())
      );

      clothesMap.computeIfAbsent(tuple.get(feedClothes.feed.id), k -> new ArrayList<>()).add(ootdDto);
    });
  }

  private Map<UUID, List<ClothesAttributeWithDefDto>> queryClothesAttributes(List<UUID> clothesIds,
      Map<UUID, List<ClothesAttributeWithDefDto>> attributeMap) {
    if (!clothesIds.isEmpty()) {
      attributeMap = queryFactory
          .from(clothesAttribute)
          .leftJoin(clothesAttribute.definition, clothesAttributeDefinition)
          .where(clothesAttribute.clothes.id.in(clothesIds))
          .transform(
              groupBy(clothesAttribute.clothes.id).as(
                  list(getClothesAttributeWithDefDto())
              )
          );
    }
    return attributeMap;
  }

  private ConstructorExpression<ClothesAttributeWithDefDto> getClothesAttributeWithDefDto() {
    return Projections.constructor(ClothesAttributeWithDefDto.class,
        clothesAttributeDefinition.id,
        clothesAttributeDefinition.name,
        clothesAttributeDefinition.selectableValues,
        clothesAttribute.attributeValue
    );
  }

  private List<UUID> getClothesIds(List<Tuple> clothesData) {
    return clothesData.stream()
        .map(tuple -> tuple.get(clothes.id))
        .distinct()
        .toList();
  }

  private List<Tuple> getClothesData(List<UUID> feedIds) {
    return queryFactory
        .select(
            feedClothes.feed.id,
            clothes.id,
            clothes.name,
            clothes.imageUrl,
            clothes.type
        )
        .from(feedClothes)
        .leftJoin(feedClothes.clothes, clothes)
        .where(feedClothes.feed.id.in(feedIds))
        .fetch();
  }

  private List<FeedDto> getFeeds(FeedPaginationRequest request, UUID likeByUserId) {
    return queryFactory
        .select(Projections.constructor(FeedDto.class,
            feed.id,
            feed.createdAt,
            feed.updatedAt,
            getUserSummary(),
            getWeatherSummaryDto(),
            Expressions.constant(List.<OotdDto>of()),
            feed.content,
            numberTemplate(Long.class, "{0}", feed.likeCount),
            numberTemplate(Integer.class, "{0}", feed.commentCount),
            likeByMe(likeByUserId)
        ))
        .from(feed)
        .leftJoin(feed.author, user)
        .leftJoin(feed.weather, weather)
        .where(
            keywordLike(request.keywordLike()),
            skyStatusEqual(request.skyStatusEqual()),
            precipitationTypeEqual(request.precipitationTypeEqual()),
            authorIdEqual(request.authorIdEqual()),
            cursor(request)
        )
        .orderBy(order(request.sortBy(), request.sortDirection()))
        .limit(pagingLimit(request.limit() + 1))
        .fetch();
  }

  private BooleanExpression likeByMe(UUID likeByUserId) {
    if (likeByUserId == null) {
      return Expressions.FALSE;
    }

    return JPAExpressions.selectOne()
        .from(feedLike)
        .where(
            feedLike.feed.id.eq(feed.id)
                .and(feedLike.user.id.eq(likeByUserId))
        )
        .exists();
  }

  @Override
  public Long feedTotalCount(FeedPaginationRequest request) {

    return queryFactory
        .select(feed.count())
        .from(feed)
        .leftJoin(feed.author, user)
        .leftJoin(feed.weather, weather)
        .where(
            keywordLike(request.keywordLike()),
            skyStatusEqual(request.skyStatusEqual()),
            precipitationTypeEqual(request.precipitationTypeEqual()),
            authorIdEqual(request.authorIdEqual())
        )
        .fetchOne();
  }

  private Predicate cursor(FeedPaginationRequest request) {
    return request.sortDirection() == SortDirection.DESCENDING
        ? desPaging(request.cursor(), request.idAfter())
        : ascPaging(request.cursor(), request.idAfter());
  }

  private Predicate desPaging(String cursor, String idAfter) {
    if (!StringUtils.hasText(cursor)) {
      return null;
    }

    LocalDateTime time = LocalDateTime.parse(cursor);
    UUID preFeedId = UUID.fromString(idAfter);

    return feed.createdAt.lt(time).or(
        feed.createdAt.eq(time)
            .and(feed.id.lt(preFeedId))
    );
  }

  private Predicate ascPaging(String cursor, String idAfter) {
    if (!StringUtils.hasText(cursor)) {
      return null;
    }

    LocalDateTime time = LocalDateTime.parse(cursor);
    UUID preFeedId = UUID.fromString(idAfter);

    return feed.createdAt.gt(time).or(
        feed.createdAt.eq(time)
            .and(feed.id.gt(preFeedId))
    );
  }

  private Predicate authorIdEqual(UUID authorId) {
    return authorId == null ? null : user.id.eq(authorId);
  }

  private Predicate precipitationTypeEqual(PrecipitationType precipitationType) {
    return (precipitationType == null) ? null : weather.precipitation.type.eq(precipitationType);
  }

  private Predicate skyStatusEqual(SkyStatus skyStatus) {
    return (skyStatus == null) ? null : weather.skyStatus.eq(skyStatus);
  }

  private BooleanExpression keywordLike(String keywordLike) {
    if (!StringUtils.hasText(keywordLike)) {
      return null;
    }

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
        list(getOotdDto()),
        feed.content,
        numberTemplate(Long.class, "{0}", feed.likeCount),
        numberTemplate(Integer.class, "{0}", feed.commentCount),
        Expressions.constant(true)
    );
  }

  private ConstructorExpression<OotdDto> getOotdDto() {
    return Projections.constructor(OotdDto.class,
        clothes.id,
        clothes.name,
        clothes.imageUrl,
        clothes.type,
        list(getClothesAttributeWithDefDto())
    );
  }

  private long pagingLimit(@NotNull Integer limit) {
    return (limit == 0) ? 20L : limit;
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
      @NotNull SortBy sortBy,
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

  private boolean isSortByCreatedAt(SortBy sortBy) {
    return sortBy.equals(SortBy.createdAt);
  }

}