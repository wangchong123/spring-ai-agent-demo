package com.demo.agent.tool;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 把所有 @Tool 注解的 Bean 统一管理，给 Skill / SubAgent 按名取用。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ToolRegistry {

    private final CommonTools commonTools;
    private final FinanceTools financeTools;

    private final Map<String, ToolCallback> all = new LinkedHashMap<>();

    @PostConstruct
    public void init() {
        register(commonTools);
        register(financeTools);
        log.info("ToolRegistry initialized with {} tools: {}", all.size(), all.keySet());
    }

    private void register(Object bean) {
        ToolCallback[] callbacks = ToolCallbacks.from(bean);
        for (ToolCallback cb : callbacks) {
            all.put(cb.getToolDefinition().name(), cb);
        }
    }

    public List<ToolCallback> pick(Collection<String> names) {
        if (names == null || names.isEmpty()) return List.of();
        List<ToolCallback> out = new ArrayList<>();
        for (String n : names) {
            ToolCallback cb = all.get(n);
            if (cb != null) out.add(cb);
        }
        return out;
    }

    public List<ToolCallback> all() {
        return new ArrayList<>(all.values());
    }

    public Set<String> names() {
        return Collections.unmodifiableSet(all.keySet());
    }
}
