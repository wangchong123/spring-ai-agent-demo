package com.demo.agent.skill;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SkillRouter {

    private final SkillLoader loader;

    public Optional<Skill> route(String userQuery) {
        if (userQuery == null) return Optional.empty();
        // 关键词命中（演示版；生产可加 embedding 语义匹配）
        for (Skill s : loader.getSkills().values()) {
            for (String t : s.triggers()) {
                if (t == null || t.isBlank()) continue;
                if (userQuery.contains(t)) return Optional.of(s);
            }
        }
        return Optional.empty();
    }
}
