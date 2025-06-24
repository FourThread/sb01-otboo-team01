package com.fourthread.ozang.module.domain.feed.entity;

import com.fourthread.ozang.module.domain.BaseUpdatableEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import lombok.Getter;

@Getter
@Entity
public class Feed extends BaseUpdatableEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  private User author;

  @ManyToOne(fetch = FetchType.LAZY)
  private WeatherData weather;

  private String content;
  private AtomicInteger likeCount;
  private AtomicInteger commentCount;

  public int increaseLike() {
    return likeCount.incrementAndGet();
  }

  public int decreaseLike() {
    return likeCount.decrementAndGet();
  }

  public int increaseComment() {
    return commentCount.incrementAndGet();
  }
}
