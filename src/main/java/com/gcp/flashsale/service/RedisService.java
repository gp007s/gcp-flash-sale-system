package com.gcp.flashsale.service;

import java.util.Collections;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

/**
 * Stock counters in Memorystore for Redis (replaces ElastiCache).
 *
 * The reserve step is ONE Lua script, so "check and take" is a single atomic
 * action on the Redis server. Two VMs can never take the same last item.
 */
@Service
public class RedisService {

    private static final String STOCK_PREFIX = "stock:";

    // Returns the remaining count after the reservation, or -1 if there is not enough stock.
    private static final DefaultRedisScript<Long> RESERVE_SCRIPT = new DefaultRedisScript<>(
            "local c = tonumber(redis.call('GET', KEYS[1]) or '0') " +
                    "local q = tonumber(ARGV[1]) " +
                    "if c < q then return -1 end " +
                    "return redis.call('DECRBY', KEYS[1], q)",
            Long.class);

    private final StringRedisTemplate redis;

    public RedisService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public static String stockKey(String itemName) {
        return STOCK_PREFIX + itemName;
    }

    /** Takes {@code quantity} items. Returns the remaining count (0 or more), or -1 if sold out. */
    public long checkItemAvailabilityAndReserve(String key, int quantity) {
        Long result = redis.execute(RESERVE_SCRIPT, Collections.singletonList(key), Integer.toString(quantity));
        return result == null ? -1 : result;
    }

    /** Admin: set the maximum stock for an item (overwrites the current value). */
    public void loadTheKeyValueFirstTime(String key, long quantity) {
        redis.opsForValue().set(key, Long.toString(quantity));
    }

    public long getAvailableItemCount(String itemName) {
        String value = redis.opsForValue().get(stockKey(itemName));
        return value == null ? 0 : Long.parseLong(value);
    }

    /** Give items back to stock (used when an order fails after the reserve). */
    public void increment(String key, int quantity) {
        redis.opsForValue().increment(key, quantity);
    }
}