package ua.dymohlo.sportPredictions.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class MatchService {
    private final RedisTemplate<String, Object> redisTemplate;

    public String getMatchesFromCacheByDate(String targetDate) {
        String cacheKey = formatDateForCache(targetDate);
        log.info("🔍 Looking for cache key: {}", cacheKey);

        Object cachedValue = redisTemplate.opsForValue().get(cacheKey);

        if (cachedValue instanceof String json) {
            log.info("✅ Found matches in cache");
            return json;
        }

        log.warn("❌ No matches found for key: {}", cacheKey);
        return "[]";
    }


    private String formatDateForCache(String isoDate) {
        String[] parts = isoDate.split("-");
        return "matches_" + parts[2] + "_" + parts[1] + "_" + parts[0];
    }
}
