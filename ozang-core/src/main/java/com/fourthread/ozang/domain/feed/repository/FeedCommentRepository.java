package com.fourthread.ozang.domain.feed.repository;

import com.fourthread.ozang.domain.feed.entity.FeedComment;
import com.fourthread.ozang.domain.feed.repository.FeedCommentRepositoryCustom;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedCommentRepository extends JpaRepository<FeedComment, UUID>,
    FeedCommentRepositoryCustom {

  List<FeedComment> findByFeed_IdAndAuthor_Id(UUID feedId, UUID authorId);
}
