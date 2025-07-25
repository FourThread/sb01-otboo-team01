package com.ozang.common.domain.feed.elasticsearch.repository;

import com.ozang.common.domain.feed.elasticsearch.entity.FeedDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface FeedElasticsearchRepository extends ElasticsearchRepository<FeedDocument, String> {

}
