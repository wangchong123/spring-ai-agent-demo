package com.demo.agent.model.cost;

import com.demo.agent.model.ModelDescriptor;
import com.demo.agent.model.ModelRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 简化版成本追踪器：累加到 Redis，便于在 /api/chat/cost 查询。
 * 真实生产可换成 JdbcTemplate 写 Postgres 的 llm_cost 表（init.sql 已建好）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CostTracker {

    private final ModelRegistry registry;
    private final StringRedisTemplate redis;
    private final AtomicLong inMemTotalRequests = new AtomicLong();

    public void record(String modelKey, String sessionId, int inputTokens, int outputTokens) {
        ModelDescriptor d = registry.descriptor(modelKey);
        if (d == null) return;
        double cost = (inputTokens * d.inputPrice() + outputTokens * d.outputPrice()) / 1_000_000d;
        try {
            redis.opsForHash().increment("llm:cost:total", modelKey + ":input_tokens", inputTokens);
            redis.opsForHash().increment("llm:cost:total", modelKey + ":output_tokens", outputTokens);
            redis.opsForHash().increment("llm:cost:total",
                    modelKey + ":cost_yuan_x1m", (long) Math.round(cost * 1_000_000));
        } catch (Exception e) {
            log.debug("[cost] redis fail: {}", e.getMessage());
        }
        inMemTotalRequests.incrementAndGet();
        log.debug("[cost] model={} in={} out={} cost={}元", modelKey, inputTokens, outputTokens, cost);
    }

    public long totalRequests() {
        return inMemTotalRequests.get();
    }
}
