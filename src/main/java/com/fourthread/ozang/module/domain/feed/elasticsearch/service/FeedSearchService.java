package com.fourthread.ozang.module.domain.feed.elasticsearch.service;

import static co.elastic.clients.elasticsearch._types.query_dsl.Operator.And;
import static co.elastic.clients.elasticsearch._types.query_dsl.Operator.Or;
import static com.fourthread.ozang.module.domain.feed.elasticsearch.entity.SearchField.ASC;
import static com.fourthread.ozang.module.domain.feed.elasticsearch.entity.SearchField.CHOSUNG;
import static com.fourthread.ozang.module.domain.feed.elasticsearch.entity.SearchField.CONTENT;
import static com.fourthread.ozang.module.domain.feed.elasticsearch.entity.SearchField.CREATED_AT;
import static com.fourthread.ozang.module.domain.feed.elasticsearch.entity.SearchField.DESC;
import static com.fourthread.ozang.module.domain.feed.elasticsearch.entity.SearchField.ENG_TO_HAN;
import static com.fourthread.ozang.module.domain.feed.elasticsearch.entity.SearchField.FUZZ_AUTO;
import static com.fourthread.ozang.module.domain.feed.elasticsearch.entity.SearchField.HAN_TO_ENG;
import static com.fourthread.ozang.module.domain.feed.elasticsearch.entity.SearchField.ID;
import static com.fourthread.ozang.module.domain.feed.elasticsearch.entity.SearchField.JAMO;
import static com.fourthread.ozang.module.domain.feed.elasticsearch.entity.SearchField.LIKE_COUNT;
import static com.fourthread.ozang.module.domain.feed.elasticsearch.entity.SearchField.PRECIPITATION;
import static com.fourthread.ozang.module.domain.feed.elasticsearch.entity.SearchField.SKY_STATUS;
import static com.fourthread.ozang.module.domain.feed.entity.SortDirection.ASCENDING;
import static com.fourthread.ozang.module.domain.feed.entity.SortDirection.DESCENDING;

import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery.Builder;
import co.elastic.clients.elasticsearch._types.query_dsl.FuzzyQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchAllQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.PrefixQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.WildcardQuery;
import com.fourthread.ozang.module.domain.BaseEntity;
import com.fourthread.ozang.module.domain.clothes.dto.response.ClothesAttributeWithDefDto;
import com.fourthread.ozang.module.domain.clothes.dto.response.OotdDto;
import com.fourthread.ozang.module.domain.clothes.entity.Clothes;
import com.fourthread.ozang.module.domain.clothes.repository.ClothesRepository;
import com.fourthread.ozang.module.domain.feed.dto.FeedData;
import com.fourthread.ozang.module.domain.feed.dto.FeedDto;
import com.fourthread.ozang.module.domain.feed.dto.request.FeedPaginationRequest;
import com.fourthread.ozang.module.domain.feed.elasticsearch.entity.FeedDocument;
import com.fourthread.ozang.module.domain.feed.elasticsearch.entity.SearchField;
import com.fourthread.ozang.module.domain.feed.elasticsearch.repository.FeedElasticsearchRepository;
import com.fourthread.ozang.module.domain.feed.entity.Feed;
import com.fourthread.ozang.module.domain.feed.entity.FeedClothes;
import com.fourthread.ozang.module.domain.feed.entity.SortBy;
import com.fourthread.ozang.module.domain.feed.entity.SortDirection;
import com.fourthread.ozang.module.domain.feed.repository.FeedClothesRepository;
import com.fourthread.ozang.module.domain.user.dto.data.UserSummary;
import com.fourthread.ozang.module.domain.user.entity.User;
import com.fourthread.ozang.module.domain.user.repository.UserRepository;
import com.fourthread.ozang.module.domain.weather.dto.PrecipitationDto;
import com.fourthread.ozang.module.domain.weather.dto.TemperatureDto;
import com.fourthread.ozang.module.domain.weather.dto.WeatherSummaryDto;
import com.fourthread.ozang.module.domain.weather.entity.Weather;
import com.fourthread.ozang.module.domain.weather.repository.WeatherRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.UncategorizedElasticsearchException;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
/// =============== Elasticsearch 활성화 조건 추가 ===============
@ConditionalOnProperty(name = "elasticsearch.enabled", havingValue = "true", matchIfMissing = false)
public class FeedSearchService {

  private final FeedElasticsearchRepository elasticsearchRepository;
  private final ElasticsearchOperations elasticsearchOperations;
  private final UserRepository userRepository;
  private final WeatherRepository weatherRepository;
  private final ClothesRepository clothesRepository;
  private final FeedClothesRepository feedClothesRepository;

  /**
   * @methodName : create
   * @date : 2025. 7. 7. PM 5:01
   * @author : wongil
   * @Description: Elasticsearch에 Feed Document 저장
   **/
  @Async
  public CompletableFuture<FeedDocument> create(Feed feed) {
    List<String> clothesIds = getClothesIds(feed);

    FeedDocument document = FeedDocument.from(feed, clothesIds);
    elasticsearchRepository.save(document);

    return CompletableFuture.completedFuture(document);
  }

  /**
   * @methodName : elasticSearch
   * @date : 2025-07-08 오전 10:15
   * @author : wongil
   * @Description: 피드 검색
   **/
  public FeedData elasticSearch(FeedPaginationRequest request) {
    SearchHits<FeedDocument> searchHits = searchFeedDocument(request);

    return toFeedData(searchHits, request);
  }

  private FeedData toFeedData(SearchHits<FeedDocument> searchHits, FeedPaginationRequest request) {
    List<FeedDocument> documents = searchHits.getSearchHits().stream()
        .map(SearchHit::getContent)
        .toList();

    boolean hasNext = documents.size() > request.limit();
    List<FeedDocument> pagedDocuments = hasNext ? documents.subList(0, request.limit()) : documents;

    Map<UUID, User> users = findAllUsers(pagedDocuments);
    Map<UUID, Weather> weathers = findAllWeathers(pagedDocuments);

    List<FeedDto> data = createFeedDtoList(pagedDocuments, users, weathers);

    String nextCursor = null;
    UUID nextIdAfter = null;

    if (hasNext && !pagedDocuments.isEmpty()) {
      FeedDocument lastDocument = pagedDocuments.get(pagedDocuments.size() - 1);
      nextCursor = lastDocument.getCreatedAt().toString();
      nextIdAfter = UUID.fromString(lastDocument.getId());
    }

    return new FeedData(
        data,
        nextCursor,
        nextIdAfter,
        hasNext,
        getTotalCount(request),
        request.sortBy(),
        request.sortDirection()
    );
  }

  private Long getTotalCount(FeedPaginationRequest request) {
    FeedPaginationRequest noneLimitRequest = FeedPaginationRequest.builder()
        .idAfter(request.idAfter())
        .sortBy(request.sortBy())
        .sortDirection(request.sortDirection())
        .keywordLike(request.keywordLike())
        .skyStatusEqual(request.skyStatusEqual())
        .precipitationTypeEqual(request.precipitationTypeEqual())
        .authorIdEqual(request.authorIdEqual())
        .limit(0)
        .build();

    return searchFeedDocument(noneLimitRequest).getTotalHits();
  }

  private List<FeedDto> createFeedDtoList(List<FeedDocument> pagedDocuments, Map<UUID, User> users,
      Map<UUID, Weather> weathers) {
    return pagedDocuments.stream()
        .map(document -> {
          User user = users.get(UUID.fromString(document.getAuthorId()));
          Weather weather = weathers.get(UUID.fromString(document.getWeatherId()));
          List<OotdDto> ootds = getOotds(document);

          return new FeedDto(
              UUID.fromString(document.getId()),
              document.getCreatedAt(),
              null,
              getUserSummary(user),
              getWeatherSummaryDto(weather),
              ootds,
              document.getContent(),
              document.getLikeCount(),
              document.getCommentCount(),
              null
          );
        })
        .toList();
  }

  private List<OotdDto> getOotds(FeedDocument document) {
    List<Clothes> clothes = findAllClothes(document);
    return clothes.stream()
        .map(cloth -> new OotdDto(
            cloth.getId(),
            cloth.getName(),
            cloth.getImageUrl(),
            cloth.getType(),
            getClothesAttributeWithDefDtos(cloth)
        ))
        .toList();
  }

  private List<ClothesAttributeWithDefDto> getClothesAttributeWithDefDtos(Clothes cloth) {
    return cloth.getAttributes().stream()
        .map(attribute ->
            new ClothesAttributeWithDefDto(
                attribute.getDefinition().getId(),
                attribute.getDefinition().getName(),
                attribute.getDefinition().getSelectableValues(),
                attribute.getAttributeValue()
            )
        )
        .toList();
  }

  private List<Clothes> findAllClothes(FeedDocument document) {
    List<UUID> clothesIds = document.getClothesIds().stream()
        .map(UUID::fromString)
        .toList();
    return clothesRepository.findAllByIdIn(clothesIds);
  }

  private WeatherSummaryDto getWeatherSummaryDto(Weather weather) {
    return new WeatherSummaryDto(
        weather.getId(),
        weather.getSkyStatus(),
        getPrecipitation(weather),
        getTemperature(weather)
    );
  }

  private UserSummary getUserSummary(User user) {
    return new UserSummary(
        user.getId(),
        user.getName(),
        user.getProfile().getProfileImageUrl()
    );
  }

  private TemperatureDto getTemperature(Weather weather) {
    return new TemperatureDto(
        weather.getTemperature().current(),
        weather.getTemperature().comparedToDayBefore(),
        weather.getTemperature().min(),
        weather.getTemperature().max()
    );
  }

  private PrecipitationDto getPrecipitation(Weather weather) {
    return new PrecipitationDto(
        weather.getPrecipitation().type(),
        weather.getPrecipitation().amount(),
        weather.getPrecipitation().probability()
    );
  }

  private Map<UUID, Weather> findAllWeathers(List<FeedDocument> pagedDocuments) {
    List<UUID> weatherIds = pagedDocuments.stream()
        .map(document -> UUID.fromString(document.getWeatherId()))
        .toList();

    return weatherRepository.findALlByIdIn(weatherIds).stream()
        .collect(Collectors.toMap(
            BaseEntity::getId,
            Function.identity()
        ));
  }

  private Map<UUID, User> findAllUsers(List<FeedDocument> pagedDocuments) {
    List<UUID> userIds = pagedDocuments.stream()
        .map(document -> UUID.fromString(document.getAuthorId()))
        .toList();

    return userRepository.findAllByIdIn(userIds).stream()
        .collect(Collectors.toMap(BaseEntity::getId,
            Function.identity()
        ));

  }

  private SearchHits<FeedDocument> searchFeedDocument(FeedPaginationRequest request) {

    List<Query> mustQueries = new ArrayList<>();
    List<Query> filterQueries = new ArrayList<>();

    // 조건에 맞춰 검색
    searchText(request, mustQueries);
    searchSkyStatus(request, filterQueries);
    searchPrecipitationType(request, filterQueries);
    searchCursor(request, filterQueries);

    // must, filter, match all 옵션 builder에 추가
    BoolQuery.Builder queryBuilder = new BoolQuery.Builder();
    mustQuery(mustQueries, queryBuilder);
    filterQuery(filterQueries, queryBuilder);
    matchAllQuery(mustQueries, filterQueries, queryBuilder);

    // elasticsearch 쿼리 빌드
    Query query = queryBuilder.build()._toQuery();

    List<SortOptions> sortOptions = sortQuery(request.sortBy(), request.sortDirection());

    // 네이티브 쿼리로 빌드
    NativeQuery searchQuery = NativeQuery.builder()
        .withQuery(query)
        .withPageable(PageRequest.of(0, request.limit() + 1))
        .withSort(sortOptions)
        .build();

    // 실제 검색
    try {
      return elasticsearchOperations.search(searchQuery, FeedDocument.class);
    } catch (UncategorizedElasticsearchException e) {
      Throwable cause = e.getCause();
      if (cause instanceof co.elastic.clients.elasticsearch._types.ElasticsearchException esEx) {
        esEx.error().rootCause().forEach(rc ->
            log.error("rootCause type={}, reason={}", rc.type(), rc.reason())
        );
      }
      throw e;
    }
  }

  private List<SortOptions> sortQuery(SortBy sortBy, SortDirection sortDirection) {
    SortOrder sortOrder = sortDirection == DESCENDING ? SortOrder.Desc : SortOrder.Asc;

    List<SortOptions> sortOptions;
    if (sortBy == SortBy.createdAt) {
      sortOptions = createSortOptions(CREATED_AT, sortOrder);
    } else {
      sortOptions = createSortOptions(LIKE_COUNT, sortOrder);
    }

    return sortOptions;
  }

  private List<SortOptions> createSortOptions(SearchField createdAt, SortOrder sortOrder) {
    return List.of(
        SortOptions.of(sort -> sort
            .field(field -> field
                .field(createdAt.toString())
                .order(sortOrder))),
        SortOptions.of(sort -> sort
            .field(field -> field
                .field(ID.toString())
                .order(SortOrder.Asc)))
    );
  }

  /**
   * @methodName : matchAllQueru
   * @date : 2025-07-08 오전 11:27
   * @author : wongil
   * @Description: 만족하는 쿼리 없으면 match all
   **/
  private void matchAllQuery(List<Query> mustQueries, List<Query> filterQueries,
      Builder queryBuilder) {
    if (mustQueries.isEmpty() && filterQueries.isEmpty()) {
      queryBuilder.must(MatchAllQuery.of(match -> match)._toQuery());
    }
  }

  /**
   * @methodName : filterQuery
   * @date : 2025-07-08 오전 11:20
   * @author : wongil
   * @Description: filter 연산
   **/
  private void filterQuery(List<Query> filterQueries, Builder queryBuilder) {
    if (!filterQueries.isEmpty()) {
      queryBuilder.filter(filterQueries);
    }
  }

  /**
   * @methodName : mustQuery
   * @date : 2025-07-08 오전 11:20
   * @author : wongil
   * @Description: must 연산
   **/
  private void mustQuery(List<Query> mustQueries, Builder queryBuilder) {
    if (!mustQueries.isEmpty()) {
      queryBuilder.must(mustQueries);
    }
  }

  /**
   * @methodName : searchCursor
   * @date : 2025-07-08 오전 11:17
   * @author : wongil
   * @Description: 커서 페이지네이션
   **/
  private void searchCursor(FeedPaginationRequest request, List<Query> filterQueries) {
    if (StringUtils.hasText(request.cursor())) {
      Query query = createCursorQuery(request);

      if (query != null) {
        filterQueries.add(query);
      }
    }
  }

  /**
   * @methodName : createCursorQuery
   * @date : 2025-07-08 오전 11:17
   * @author : wongil
   * @Description: 커서 페이지네이션 실제 쿼리
   **/
  private Query createCursorQuery(FeedPaginationRequest request) {
    SortDirection sortDirection = request.sortDirection();
    LocalDateTime cursor = LocalDateTime.parse(request.cursor());
    String idAfter = request.idAfter();

    if (sortDirection == DESCENDING) {
      RangeQuery cursorQuery = createCursorQuery(cursor, DESC.toString());

      return getPagedCursor(idAfter, cursor, cursorQuery);

    } else if (sortDirection == ASCENDING) {
      RangeQuery cursorQuery = createCursorQuery(cursor, ASC.toString());

      return getPagedCursor(idAfter, cursor, cursorQuery);
    }

    return null;
  }

  private Query getPagedCursor(String idAfter, LocalDateTime cursor, RangeQuery cursorQuery) {
    if (cursorQuery == null) {
      return null;
    }

    if (StringUtils.hasText(idAfter)) {
      Query eqCursorQuery = createEqCursorQuery(cursor);
      RangeQuery idAfterQuery = createIdAfterQuery(idAfter);
      Query idQuery = getQueryByEqCursorAndIdAfter(eqCursorQuery, idAfterQuery);

      return getQueryByCursorOrEqCursorAndIdAfter(cursorQuery, idQuery);
    } else {
      return cursorQuery._toQuery();
    }
  }

  private Query getQueryByCursorOrEqCursorAndIdAfter(RangeQuery cursorQuery, Query idQuery) {
    return BoolQuery.of(bool -> bool
        .should(cursorQuery._toQuery())
        .should(idQuery)
        .minimumShouldMatch("1")
    )._toQuery();
  }

  private Query getQueryByEqCursorAndIdAfter(Query eqCursorQuery, RangeQuery idAfterQuery) {
    return BoolQuery.of(bool -> bool
        .must(eqCursorQuery)
        .must(idAfterQuery._toQuery())
    )._toQuery();
  }

  private RangeQuery createIdAfterQuery(String idAfter) {
    return RangeQuery.of(range -> range
        .term(term -> term
            .field(ID.toString())
            .lt(idAfter)
        )
    );
  }

  private Query createEqCursorQuery(LocalDateTime cursor) {
    return TermQuery.of(term -> term
        .field(CREATED_AT.toString())
        .value(cursor.toString())
    )._toQuery();
  }

  private RangeQuery createCursorQuery(LocalDateTime cursor, String send) {
    if (DESC.toString().equals(send)) {
      return RangeQuery.of(range -> range
          .date(date -> date
              .field(CREATED_AT.toString())
              .lt(cursor.toString())
          ));
    } else if (ASC.toString().equals(send)) {
      return RangeQuery.of(range -> range
          .date(date -> date
              .field(CREATED_AT.toString())
              .gt(cursor.toString())
          ));
    }

    return null;
  }

  /**
   * @methodName : searchPrecipitationType
   * @date : 2025-07-08 오전 11:12
   * @author : wongil
   * @Description: 강수량에 따른 타입 검색
   **/
  private void searchPrecipitationType(FeedPaginationRequest request, List<Query> filterQueries) {
    if (request.precipitationTypeEqual() != null) {
      TermQuery precipitationQuery = TermQuery.of(term -> term
          .field(PRECIPITATION.toString())
          .value(request.precipitationTypeEqual().toString())
      );
      filterQueries.add(precipitationQuery._toQuery());
    }
  }

  /**
   * @methodName : searchSkyStatus
   * @date : 2025-07-08 오전 11:12
   * @author : wongil
   * @Description: 날씨 검색
   **/
  private void searchSkyStatus(FeedPaginationRequest request, List<Query> filterQueries) {
    if (request.skyStatusEqual() != null) {
      TermQuery skyStatusQuery = TermQuery.of(term -> term
          .field(SKY_STATUS.toString())
          .value(request.skyStatusEqual().toString())
      );
      filterQueries.add(skyStatusQuery._toQuery());
    }
  }

  /**
   * @methodName : searchText
   * @date : 2025-07-08 오전 11:12
   * @author : wongil
   * @Description: 텍스트 검색
   **/
  private void searchText(FeedPaginationRequest request, List<Query> mustQueries) {
    if (StringUtils.hasText(request.keywordLike())) {

      BoolQuery boolQuery = BoolQuery.of(bool -> bool
          .should(exactMatchText(request))
          .should(ngramMatchText(request))
          .should(convertFromContentToChosung(request))
          .should(convertFromEngToKo(request))
          .should(convertFromKoToEng(request))
          .should(jamoMatchText(request))
          .should(createFuzzyQuery(request))
          .minimumShouldMatch("1")
      );

      mustQueries.add(boolQuery._toQuery());
    }
  }

  private Query jamoMatchText(FeedPaginationRequest request) {
    return BoolQuery.of(bool -> bool
        .should(exactMatchJamo(request))
        .should(prefixPartialMatchJamo(request))
        .should(wildcardPartialMatchJamo(request))
    )._toQuery();
  }

  private Query wildcardPartialMatchJamo(FeedPaginationRequest request) {
    return WildcardQuery.of(wildcard -> wildcard
        .field(JAMO.toString())
        .value(request.keywordLike() + "*")
    )._toQuery();
  }

  private Query prefixPartialMatchJamo(FeedPaginationRequest request) {
    return PrefixQuery.of(prefix -> prefix
        .field(JAMO.toString())
        .value(request.keywordLike())
    )._toQuery();
  }

  private Query exactMatchJamo(FeedPaginationRequest request) {
    return MatchQuery.of(match -> match
            .field(JAMO.toString())
            .query(request.keywordLike())
            .fuzziness(FUZZ_AUTO.toString()))
        ._toQuery();
  }

  private Query convertFromKoToEng(FeedPaginationRequest request) {
    return BoolQuery.of(bool -> bool
        .should(exactMatchKoToEng(request))
        .should(partialMatchKoToEng(request))
        .should(prefixPartialMatchKoToEng(request))
    )._toQuery();
  }

  private Query partialMatchKoToEng(FeedPaginationRequest request) {
    return BoolQuery.of(bool -> bool
        .should(PrefixQuery.of(prefix -> prefix
            .field(HAN_TO_ENG.toString())
            .value(request.keywordLike())
        )._toQuery())
        .should(WildcardQuery.of(wildcard -> wildcard
            .field(HAN_TO_ENG.toString())
            .value(request.keywordLike() + "*")
        )._toQuery())
        .should(MatchQuery.of(match -> match
            .field(HAN_TO_ENG.toString())
            .query(request.keywordLike())
            .operator(Or)
        )._toQuery())
    )._toQuery();
  }

  private Query prefixPartialMatchKoToEng(FeedPaginationRequest request) {
    return PrefixQuery.of(prefix -> prefix
        .field(HAN_TO_ENG.toString())
        .value(request.keywordLike())
    )._toQuery();
  }

  private Query exactMatchKoToEng(FeedPaginationRequest request) {
    return MatchQuery.of(match -> match
        .field(HAN_TO_ENG.toString())
        .query(request.keywordLike())
        .operator(And)
    )._toQuery();
  }

  private Query convertFromEngToKo(FeedPaginationRequest request) {
    return BoolQuery.of(bool -> bool
        .should(exactMatchEngToKo(request))
        .should(partialMatchEngToKo(request))
        .should(prefixPartialMatchEngToKo(request))
    )._toQuery();
  }

  private Query partialMatchEngToKo(FeedPaginationRequest request) {
    return BoolQuery.of(bool -> bool
        .should(PrefixQuery.of(prefix -> prefix
            .field(ENG_TO_HAN.toString())
            .value(request.keywordLike())
        )._toQuery())
        .should(WildcardQuery.of(wildcard -> wildcard
            .field(ENG_TO_HAN.toString())
            .value(request.keywordLike() + "*")
        )._toQuery())
        .should(MatchQuery.of(match -> match
            .field(ENG_TO_HAN.toString())
            .query(request.keywordLike())
            .operator(Or)
        )._toQuery())
    )._toQuery();
  }

  private Query prefixPartialMatchEngToKo(FeedPaginationRequest request) {
    return PrefixQuery.of(prefix -> prefix
        .field(ENG_TO_HAN.toString())
        .value(request.keywordLike())
    )._toQuery();
  }

  private Query exactMatchEngToKo(FeedPaginationRequest request) {
    return MatchQuery.of(match -> match
        .field(ENG_TO_HAN.toString())
        .query(request.keywordLike())
        .operator(And)
    )._toQuery();
  }

  private Query convertFromContentToChosung(FeedPaginationRequest request) {
    return BoolQuery.of(bool -> bool
        .should(exactMatchChosung(request))
        .should(prefixPartialMatchChosung(request))
        .should(wildcardPartialMatchChosung(request))
    )._toQuery();
  }

  private Query wildcardPartialMatchChosung(FeedPaginationRequest request) {
    return WildcardQuery.of(wildcard -> wildcard
            .field(CHOSUNG.toString())
            .value(request.keywordLike() + "*"))
        ._toQuery();
  }

  private Query prefixPartialMatchChosung(FeedPaginationRequest request) {
    return PrefixQuery.of(prefix -> prefix
            .field(CHOSUNG.toString())
            .value(request.keywordLike()))
        ._toQuery();
  }

  private Query exactMatchChosung(FeedPaginationRequest request) {
    return MatchQuery.of(match -> match
        .field(CHOSUNG.toString())
        .query(request.keywordLike())
        .operator(And)
    )._toQuery();
  }

  private Query ngramMatchText(FeedPaginationRequest request) {
    return MatchQuery.of(match -> match
        .field(CONTENT.toString() + ".ngram")
        .query(request.keywordLike())
    )._toQuery();
  }

  private Query exactMatchText(FeedPaginationRequest request) {
    return MatchQuery.of(match -> match
            .field(CONTENT.toString())
            .query(request.keywordLike()))
        ._toQuery();
  }

  private Query createFuzzyQuery(FeedPaginationRequest request) {

    String fuzziness;
    int maxExpansion;
    int keywordLength = request.keywordLike().length();

    if (keywordLength <= 3) {
      fuzziness = "0";
      maxExpansion = 10;
    } else if (keywordLength <= 5) {
      fuzziness = "1";
      maxExpansion = 20;
    } else {
      fuzziness = "2";
      maxExpansion = 30;
    }

    return FuzzyQuery.of(fuzzy -> fuzzy
            .field(CONTENT.toString())
            .value(request.keywordLike())
            .fuzziness(fuzziness)
            .prefixLength(0)
            .maxExpansions(maxExpansion)
            .transpositions(true)
        )
        ._toQuery();
  }

  private List<String> getClothesIds(Feed feed) {
    List<FeedClothes> feedClothes = feedClothesRepository.findAllByFeed(feed);

    return feedClothes.stream()
        .map(feedCloth -> feedCloth.getClothes().getId().toString())
        .toList();
  }
}
