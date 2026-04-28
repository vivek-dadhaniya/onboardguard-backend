package com.onboardguard;

import com.onboardguard.watchlist.elasticsearch.WatchlistSearchRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

@SpringBootTest
@ActiveProfiles("test")
class OnboardguardBackendApplicationTests {

    // 1. Mock the ES Repository: This is the one causing the "UnsatisfiedDependencyException"
    @MockitoBean
    private WatchlistSearchRepository watchlistSearchRepository;

    // 2. Mock ES Operations: Used by your search services
    @MockitoBean
    private ElasticsearchOperations elasticsearchOperations;

    // 3. Mock Redis: Prevents "Connection refused" errors during context load
    @MockitoBean
    private RedisConnectionFactory redisConnectionFactory;

    // NOTE: We do NOT mock WatchlistRepository (JPA).
    // We let it use the H2 database defined in application-test.yml.

    @Test
    void contextLoads() {
        // This confirms the entire app logic, security, and DB are wired correctly.
    }
}