package com.fourthread.ozang.module.domain.feed.entity;

import com.fourthread.ozang.module.domain.BaseUpdatableEntity;
import com.fourthread.ozang.module.domain.clothes.entity.Clothes;
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
