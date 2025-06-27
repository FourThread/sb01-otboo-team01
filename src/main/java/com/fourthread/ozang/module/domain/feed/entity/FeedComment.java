package com.fourthread.ozang.module.domain.feed.entity;

import static org.hibernate.annotations.OnDeleteAction.CASCADE;

import com.fourthread.ozang.module.domain.BaseUpdatableEntity;
import com.fourthread.ozang.module.domain.user.entity.User;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class FeedComment extends BaseUpdatableEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @OnDelete(action = CASCADE)
  private Feed feed;

  @ManyToOne(fetch = FetchType.LAZY)
  private User author;

  private String content;
}
