package com.demo.agent.model.router;

import com.demo.agent.model.ModelRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ModelRouter {

    private final ManualStrategy manual;
    private final SessionStrategy session;
    private final AutoStrategy auto;
    private final ModelRegistry registry;

    /** 优先级：手动 > 会话 > 自动 */
    public String pickModel(RoutingContext ctx) {
        String key = manual.resolve(ctx);
        if (key == null) key = session.resolve(ctx);
        if (key == null) key = auto.resolve(ctx);
        if (key == null || !registry.keys().contains(key)) {
            log.warn("Unknown or null modelKey [{}], fallback to default", key);
            key = registry.keys().iterator().next();
        }
        return key;
    }

    /** 主模型失败 → 自动降级 */
    public String fallback(String failed) {
        return switch (failed) {
            case "deepseek-pro"   -> "qwen-max";
            case "deepseek-flash" -> "qwen-flash";
            case "qwen-max", "qwen-plus" -> "deepseek-pro";
            case "qwen-flash"     -> "deepseek-flash";
            case "glm-5"          -> "deepseek-pro";
            case "glm-flash"      -> "qwen-flash";
            default               -> registry.keys().iterator().next();
        };
    }
}
