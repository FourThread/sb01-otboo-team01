package com.ozang.common.domain.feed.entity;

import com.ozang.common.domain.BaseUpdatableEntity;
import com.ozang.common.domain.clothes.entity.Clothes;
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
