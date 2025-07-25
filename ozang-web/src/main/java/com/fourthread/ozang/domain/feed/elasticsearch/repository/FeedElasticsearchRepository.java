package com.fourthread.ozang.domain.feed.elasticsearch.repository;

import com.fourthread.ozang.domain.feed.elasticsearch.entity.FeedDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface FeedElasticsearchRepository extends ElasticsearchRepository<FeedDocument, String> {

}
