package com.fourthread.ozang.module.domain.feed.repository;

import com.fourthread.ozang.module.domain.feed.entity.Feed;
import com.fourthread.ozang.module.domain.feed.entity.FeedClothes;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedClothesRepository extends JpaRepository<FeedClothes, UUID> {

  List<FeedClothes> findAllByClothes_id(UUID clothesId);

  List<FeedClothes> findAllByFeed_Id(UUID feedId);

  List<FeedClothes> findAllByFeed(Feed feed);
}
