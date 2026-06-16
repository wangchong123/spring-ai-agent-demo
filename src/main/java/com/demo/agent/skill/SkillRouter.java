package com.demo.agent.skill;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Skill 路由：双层匹配。
 *   1) 关键词命中（trigger 子串匹配）—— 当前已实现
 *   2) 语义命中（embedding 相似度）—— 接口已留出，等接入 EmbeddingModel 后补 SemanticSkillMatcher
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SkillRouter {

    private final SkillLoader loader;

    public Optional<Skill> route(String userQuery) {
        if (userQuery == null) return Optional.empty();
        // Layer 1: keyword
        for (Skill s : loader.getSkills().values()) {
            for (String t : s.triggers()) {
                if (t == null || t.isBlank()) continue;
                if (userQuery.contains(t)) {
                    log.debug("[skill-router] keyword hit: {} via trigger '{}'", s.name(), t);
                    return Optional.of(s);
                }
            }
        }
        // Layer 2: semantic — TODO: 接入 EmbeddingModel 后补；此处保持开放，便于 PR 增量
        return Optional.empty();
    }
}
