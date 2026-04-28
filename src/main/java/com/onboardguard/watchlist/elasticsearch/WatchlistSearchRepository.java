package com.onboardguard.watchlist.elasticsearch;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
//import org.springframework.stereotype.Repository;
import java.util.List;

//@Repository
public interface WatchlistSearchRepository extends ElasticsearchRepository<WatchlistDocument , String> {
    // Elasticsearch generates the DSL query automatically from this method name
    List<WatchlistDocument> findByPrimaryNameOrAliasesAndIsActiveTrue(String name, String alias);
}
