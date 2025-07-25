package com.fourthread.ozang.app.domain.feed.event;

import com.fourthread.ozang.app.domain.feed.entity.Feed;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class FeedCreatedEvent {

  private final Feed feed;
  private final List<String> clothesIds;

}
