package com.fourthread.ozang.module.domain.feed.repository;

import com.fourthread.ozang.module.domain.feed.entity.Feed;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedRepository extends JpaRepository<Feed, UUID>, FeedRepositoryCustom {

}
