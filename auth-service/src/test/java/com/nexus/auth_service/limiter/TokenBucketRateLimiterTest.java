package com.nexus.auth_service.limiter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.Jedis;

import static org.junit.jupiter.api.Assertions.*;

class TokenBucketRateLimiterTest {

    private TokenBucketRateLimiter rateLimiter;
    private Jedis jedis;

    @BeforeEach
    void setUp() {
        jedis = new Jedis("localhost", 6379);
        // Clear rate limiter keys before each test
        java.util.Set<String> keys = jedis.keys("rate_limit:*");
        for (String key : keys) {
            jedis.del(key);
        }
        // Initialize rate limiter with capacity 3,
        // Very long refill period (99999 seconds ~ 27 hours) so tokens effectively don't recover during test,
        // 1 token per refill period
        rateLimiter = new TokenBucketRateLimiter(jedis, 3, 99999, 1);
    }

    @AfterEach
    void tearDown() {
        if (jedis != null) {
            jedis.close();
        }
    }

    @Test
    void testIsAllowed_FirstRequest() {
        // Given
        String clientId = "test-client-first";
        
        // When
        boolean allowed = rateLimiter.isAllowed(clientId);
        
        // Then - first request should be allowed (tokens available)
        assertTrue(allowed, "First request should be allowed");
    }

    @Test
    void testIsAllowed_WithinCapacity() {
        // Given
        String clientId = "test-client-capacity";
        
        // When - make requests within capacity (3)
        boolean r1 = rateLimiter.isAllowed(clientId);
        boolean r2 = rateLimiter.isAllowed(clientId);
        boolean r3 = rateLimiter.isAllowed(clientId);
        
        // Then - first three should be allowed (capacity = 3)
        assertTrue(r1, "1st request allowed");
        assertTrue(r2, "2nd request allowed");
        assertTrue(r3, "3rd request allowed");
    }

    @Test
    void testIsAllowed_ExceedsCapacity() {
        // Given
        String clientId = "test-client-exceed";
        
        // When - make requests exceeding capacity (3)
        boolean r1 = rateLimiter.isAllowed(clientId);
        boolean r2 = rateLimiter.isAllowed(clientId);
        boolean r3 = rateLimiter.isAllowed(clientId);
        boolean r4 = rateLimiter.isAllowed(clientId);
        
        // Then - first 3 should be allowed, 4th should be denied (capacity exceeded)
        assertTrue(r1, "1st request allowed");
        assertTrue(r2, "2nd request allowed");
        assertTrue(r3, "3rd request allowed");
        assertFalse(r4, "4th request should be denied (capacity exceeded)");
    }
}