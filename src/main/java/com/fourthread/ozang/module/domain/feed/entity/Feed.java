package com.fourthread.ozang.module.domain.feed.entity;

import com.fourthread.ozang.module.domain.BaseUpdatableEntity;
import com.fourthread.ozang.module.domain.feed.dto.dummy.Weather;
import com.fourthread.ozang.module.domain.user.entity.User;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.Getter;

@Getter
@Entity
public class Feed extends BaseUpdatableEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  private User author;

  @ManyToOne(fetch = FetchType.LAZY)
  private Weather weather;

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
