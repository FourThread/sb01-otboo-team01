package com.fourthread.ozang.module.domain.feed.elasticsearch.entity;

import static org.springframework.data.elasticsearch.annotations.FieldType.Date;
import static org.springframework.data.elasticsearch.annotations.FieldType.Integer;
import static org.springframework.data.elasticsearch.annotations.FieldType.Keyword;
import static org.springframework.data.elasticsearch.annotations.FieldType.Long;
import static org.springframework.data.elasticsearch.annotations.FieldType.Text;

import com.fourthread.ozang.module.domain.feed.entity.Feed;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.Mapping;
import org.springframework.data.elasticsearch.annotations.Setting;

@Builder
@Getter
@Document(indexName = "feeds")
@Setting(settingPath = "elasticsearch/index-settings.json")
@Mapping(mappingPath = "elasticsearch/index-mappings.json")
public class FeedDocument {

  @Id
  private String id;

  @Field(type = Keyword)
  private String feedId;

  @Field(type = Keyword)
  private String authorId;

  @Field(type = Keyword)
  private String weatherId;

  @Field(type = Text)
  private String content;

  @Field(type = Long)
  private Long likeCount;

  @Field(type = Integer)
  private Integer commentCount;

  @Field(
      type = Date,
      format = {},
      pattern = "yyyy-MM-dd'T'HH:mm:ss"
  )
  private LocalDateTime createdAt;

  @Field(type = Keyword)
  private List<String> clothesIds;

  @Field(type = Keyword)
  private String skyStatus;

  @Field(type = Keyword)
  private String precipitationType;

  public static FeedDocument from(Feed feed, List<String> clothesIds) {
    return FeedDocument.builder()
        .id(feed.getId().toString())
        .feedId(feed.getId().toString())
        .authorId(feed.getAuthor().getId().toString())
        .weatherId(feed.getWeather().getId().toString())
        .content(feed.getContent())
        .likeCount(feed.getLikeCount().longValue())
        .commentCount(feed.getCommentCount().intValue())
        .createdAt(feed.getCreatedAt())
        .clothesIds(clothesIds)
        .skyStatus(feed.getWeather().getSkyStatus().name())
        .precipitationType(feed.getWeather().getPrecipitation().type().name())
        .build();
  }
}
