package com.fourthread.ozang.app.domain.feed.elasticsearch.repository;

import com.fourthread.ozang.app.domain.feed.elasticsearch.entity.FeedDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface FeedElasticsearchRepository extends ElasticsearchRepository<FeedDocument, String> {

}
