package com.example.ratelimiter.integration;

import com.example.ratelimiter.limiters.stores.RedisRateLimitStore;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
class RedisIntegrationTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.data.redis.host", redis::getHost);
        r.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired private RedisRateLimitStore store;
    @Autowired private StringRedisTemplate redisTemplate;

    @BeforeEach
    void flush() { redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll(); }

    @Test @DisplayName("Increment returns sequential counts")
    void increment() {
        String key = "rl:test:ip:1.1.1.1";
        for (int i = 1; i <= 5; i++) assertThat(store.increment(key, Duration.ofSeconds(60))).isEqualTo(i);
    }

    @Test @DisplayName("TTL is set on first increment")
    void ttl() {
        String key = "rl:test:ip:2.2.2.2";
        store.increment(key, Duration.ofSeconds(30));
        assertThat(store.getTtl(key)).isGreaterThan(0).isLessThanOrEqualTo(30);
    }

    @Test @DisplayName("101st request exceeds limit of 100")
    void limitExceeded() {
        String key = "rl:test:ip:3.3.3.3";
        for (int i = 0; i < 100; i++) store.increment(key, Duration.ofSeconds(60));
        assertThat(store.increment(key, Duration.ofSeconds(60))).isGreaterThan(100);
    }

    @Test @DisplayName("Different clients have independent counters")
    void independentClients() {
        for (int i = 0; i < 50; i++) store.increment("rl:test:ip:A", Duration.ofSeconds(60));
        assertThat(store.increment("rl:test:ip:B", Duration.ofSeconds(60))).isEqualTo(1);
    }

    @Test @DisplayName("200 concurrent requests → exactly 100 allowed (Lua atomicity)")
    void concurrency() throws InterruptedException {
        int limit = 100, total = 200;
        String key = "rl:concurrent:ip:5.5.5.5";
        var executor = Executors.newFixedThreadPool(20);
        var latch = new CountDownLatch(total);
        var allowed = new AtomicInteger();

        for (int i = 0; i < total; i++) {
            executor.submit(() -> {
                try {
                    if (store.increment(key, Duration.ofSeconds(60)) <= limit) allowed.incrementAndGet();
                } finally { latch.countDown(); }
            });
        }
        latch.await();
        executor.shutdown();

        assertThat(allowed.get()).isEqualTo(limit);
    }

    @Test @DisplayName("Sliding Window Lua — counts and slides properly")
    void slidingWindowIntegration() {
        String key = "rl:sliding:ip:6.6.6.6";
        Duration window = Duration.ofSeconds(5);
        int limit = 3;

        long t1 = System.currentTimeMillis();
        assertThat(store.checkSlidingWindow(key, window, limit, t1)).isEqualTo(1);
        assertThat(store.checkSlidingWindow(key, window, limit, t1 + 100)).isEqualTo(2);
        assertThat(store.checkSlidingWindow(key, window, limit, t1 + 200)).isEqualTo(3);

        // 4th request within 5s window should be rejected
        assertThat(store.checkSlidingWindow(key, window, limit, t1 + 300)).isEqualTo(-1);

        // Should be able to get oldest ZSet score (which is t1)
        Long oldest = store.getOldestZSetScore(key);
        assertThat(oldest).isNotNull().isEqualTo(t1);

        // A request 6s later should slide the window and succeed (it drops the first 3 requests because window is 5s)
        assertThat(store.checkSlidingWindow(key, window, limit, t1 + 6000)).isEqualTo(1);
    }

    @Test @DisplayName("Token Bucket Lua — consumes and refills properly")
    void tokenBucketIntegration() {
        String key = "rl:bucket:ip:7.7.7.7";
        int limit = 5;
        double refillRate = 1.0; // 1 token per second
        Duration window = Duration.ofSeconds(5);

        long t1 = System.currentTimeMillis();
        // First call consumes 1, returns capacity - 1 = 4 remaining
        assertThat(store.checkTokenBucket(key, limit, refillRate, t1, window)).isEqualTo(4);
        assertThat(store.checkTokenBucket(key, limit, refillRate, t1, window)).isEqualTo(3);
        assertThat(store.checkTokenBucket(key, limit, refillRate, t1, window)).isEqualTo(2);
        assertThat(store.checkTokenBucket(key, limit, refillRate, t1, window)).isEqualTo(1);
        assertThat(store.checkTokenBucket(key, limit, refillRate, t1, window)).isEqualTo(0);

        // 6th call should be rejected because bucket is empty
        assertThat(store.checkTokenBucket(key, limit, refillRate, t1, window)).isEqualTo(-1);

        // 2 seconds later, it should have refilled 2 tokens (so remaining tokens after consuming 1 should be 1)
        assertThat(store.checkTokenBucket(key, limit, refillRate, t1 + 2000, window)).isEqualTo(1);

        // Verify we can retrieve tokens value as double
        Double tokens = store.getHashDouble(key, "tokens");
        assertThat(tokens).isNotNull().isLessThanOrEqualTo(1.0);
    }

}
