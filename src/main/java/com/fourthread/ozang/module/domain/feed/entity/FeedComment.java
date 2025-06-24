package com.fourthread.ozang.module.domain.feed.entity;

import static org.hibernate.annotations.OnDeleteAction.*;

import com.fourthread.ozang.module.domain.BaseUpdatableEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import nonapi.io.github.classgraph.fastzipfilereader.LogicalZipFile;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import software.amazon.awssdk.core.pagination.sync.PaginatedResponsesIterator;

@Entity
public class FeedComment extends BaseUpdatableEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @OnDelete(action = CASCADE)
  private Feed feed;

  @ManyToOne(fetch = FetchType.LAZY)
  private User author;

  private String content;
}
