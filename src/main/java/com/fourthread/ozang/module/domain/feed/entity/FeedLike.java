package com.fourthread.ozang.module.domain.feed.entity;

import com.fourthread.ozang.module.domain.BaseEntity;
import com.fourthread.ozang.module.domain.user.entity.User;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Entity
@AllArgsConstructor
@NoArgsConstructor
public class FeedLike extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  private Feed feed;

  @ManyToOne(fetch = FetchType.LAZY)
  private User user;
}
