package com.onboardguard.shared.infrastructure;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@Configuration
// 1. Tell JPA to scan everywhere EXCEPT the elasticsearch folders
@EnableJpaRepositories(
        basePackages = "com.onboardguard",
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASPECTJ,
                pattern = "com.onboardguard..elasticsearch..*"
        )
)
// 2. Tell Elasticsearch to ONLY scan its specific folder
@EnableElasticsearchRepositories(
        basePackages = "com.onboardguard.watchlist.elasticsearch"
)
public class RepositoryConfig {
}