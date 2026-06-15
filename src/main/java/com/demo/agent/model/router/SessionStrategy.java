package com.demo.agent.model.router;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class SessionStrategy implements RoutingStrategy {

    private final StringRedisTemplate redis;

    @Override
    public String resolve(RoutingContext ctx) {
        if (ctx.sessionId() == null) return null;
        try {
            return redis.opsForValue().get("session:model:" + ctx.sessionId());
        } catch (Exception e) {
            return null;
        }
    }

    public void bind(String sessionId, String modelKey) {
        redis.opsForValue().set("session:model:" + sessionId, modelKey, Duration.ofHours(2));
    }

    public void clear(String sessionId) {
        redis.delete("session:model:" + sessionId);
    }
}
