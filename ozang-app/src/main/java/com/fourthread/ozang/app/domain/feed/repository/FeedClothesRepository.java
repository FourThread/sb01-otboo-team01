package com.fourthread.ozang.app.domain.feed.repository;

import com.fourthread.ozang.core.domain.feed.entity.Feed;
import com.fourthread.ozang.core.domain.feed.entity.FeedClothes;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedClothesRepository extends JpaRepository<FeedClothes, UUID> {

  List<FeedClothes> findAllByClothes_id(UUID clothesId);

  List<FeedClothes> findAllByFeed_Id(UUID feedId);

  List<FeedClothes> findAllByFeed(Feed feed);

  void deleteByFeed_Id(UUID feedId);
}
