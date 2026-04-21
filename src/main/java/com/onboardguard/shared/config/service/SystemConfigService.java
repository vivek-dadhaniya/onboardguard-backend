package com.onboardguard.shared.config.service;

import com.onboardguard.shared.common.exception.ResourceNotFoundException;
import com.onboardguard.shared.config.entity.SystemConfig;
import com.onboardguard.shared.config.repository.SystemConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemConfigService {

    private final SystemConfigRepository configRepository;
    private final RedisTemplate<String, String> redisTemplate;

    private final String CACHE_PREFIX = "config:";
    private final Duration CACHE_TTL = Duration.ofSeconds(60);

    // REDIS -> type-safe getters (used by everyone)

    public String getString(String key) {
        return getCached(key);
    }

    public Integer getInt(String key) {
        return Integer.parseInt(getCached(key));
    }

    public BigDecimal getBigDecimal(String key) {
        return new BigDecimal(getCached(key));
    }

    public Boolean getBoolean(String key) {
        return Boolean.parseBoolean(getCached(key));
    }


    // WRITE — called by MakerCheckerService after approval
    @Transactional
    public void update(String key, String newValue, Long updatedByUserId) {
        SystemConfig config = configRepository.findByConfigKey(key)
                .orElseThrow(() -> new ResourceNotFoundException("Config not found: " + key));

        log.info("SystemConfig update: key='{}', updatedBy=userId:{}", key, updatedByUserId);

        config.setConfigValue(newValue);
        configRepository.save(config);

        evictCache(key);    // Force Redis refresh on next read
    }

    // ADMIN DISPLAY — non-sensitive entries only
    @Transactional(readOnly = true)
    public List<SystemConfig> getAllNonSensitive() {
        return configRepository.findByIsSensitiveFalse();
    }

    // CACHE INTERNALS
    private String getCached(String key) {
        String cachedKey = CACHE_PREFIX + key;

        String cached = redisTemplate.opsForValue().get(cachedKey);
        if (cached != null) {
            log.debug("SystemConfig cache HIT: key='{}'", key);
            return cached;
        }

        log.debug("SystemConfig cache MISS: key='{}' — loading from DB", key);

        String dbValue = configRepository.findByConfigKey(key)
                .map(SystemConfig::getConfigValue)
                .orElseThrow(() -> new ResourceNotFoundException("Config key not found: " + key));

        redisTemplate.opsForValue().set(cachedKey, dbValue, CACHE_TTL);
        return dbValue;
    }

    public void evictCache(String key) {
        String cacheKey = CACHE_PREFIX + key;
        redisTemplate.delete(cacheKey);
        log.info("SystemConfig cache evicted: key='{}'", key);
    }


    public void clearAllConfigCache() {
        // Find all keys in Redis that start with "config:"
        Set<String> keys = redisTemplate.keys(CACHE_PREFIX + "*");

        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.warn("ADMIN ACTION: SystemConfig cache completely flushed. {} keys removed.", keys.size());
        } else {
            log.info("SystemConfig cache flush requested, but cache was already empty.");
        }
    }
}
