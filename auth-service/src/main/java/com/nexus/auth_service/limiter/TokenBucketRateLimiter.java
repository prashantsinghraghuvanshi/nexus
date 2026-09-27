package com.nexus.auth_service.limiter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import java.time.Duration;

@Component
public class TokenBucketRateLimiter {

    private final Jedis jedis;
    private final int bucketCapacity;
    private final long refillPeriodSeconds;
    private final int refillTokensPerPeriod;

    public TokenBucketRateLimiter(Jedis jedis,
                                  @Value("${rate.limiter.bucket-capacity:100}") int bucketCapacity,
                                  @Value("${rate.limiter.refill-period:1}") long refillPeriodSeconds,
                                  @Value("${rate.limiter.refill-tokens:10}") int refillTokensPerPeriod) {
        this.jedis = jedis;
        this.bucketCapacity = bucketCapacity;
        this.refillPeriodSeconds = refillPeriodSeconds;
        this.refillTokensPerPeriod = refillTokensPerPeriod;
    }

    public boolean isAllowed(String clientId) {
        String keyCount = "rate_limit:" + clientId + ":count";
        String keyLastRefill = "rate_limit:" + clientId + ":lastRefill";

        long currentTime = System.currentTimeMillis();
        String lastRefillStr = jedis.get(keyLastRefill);
        long lastRefill = lastRefillStr != null ? Long.parseLong(lastRefillStr) : currentTime;
        
        // Refill tokens based on elapsed time
        long elapsedSeconds = (currentTime - lastRefill) / 1000;
        int tokensToAdd = (int) (elapsedSeconds * ((double) refillTokensPerPeriod / refillPeriodSeconds));
        
        // Get current count, default to bucketCapacity if key doesn't exist
        String currentCountStr = jedis.get(keyCount);
        int currentCount = currentCountStr != null ? Integer.parseInt(currentCountStr) : bucketCapacity;
        currentCount = Math.min(bucketCapacity, currentCount + tokensToAdd);

        boolean isAllowed = currentCount > 0;
        if (isAllowed) {
            currentCount--;
        }

        // Update in transaction
        Transaction transaction = jedis.multi();
        transaction.set(keyLastRefill, String.valueOf(currentTime));
        transaction.set(keyCount, String.valueOf(currentCount));
        transaction.exec();

        return isAllowed;
    }
}