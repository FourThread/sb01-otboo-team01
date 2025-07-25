package com.fourthread.ozang.app.domain.feed.entity;

import com.fourthread.ozang.app.domain.clothes.entity.Clothes;
import com.fourthread.ozang.core.domain.BaseUpdatableEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "feed_clothes")
public class FeedClothes extends BaseUpdatableEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  private Clothes clothes;

  @ManyToOne(fetch = FetchType.LAZY)
  private Feed feed;
}
