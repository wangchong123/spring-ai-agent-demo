package com.demo.agent.skill;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class SkillLoader {

    @Getter
    private final Map<String, Skill> skills = new LinkedHashMap<>();

    private final ObjectMapper yaml = new ObjectMapper(new YAMLFactory());

    @PostConstruct
    public void load() {
        var resolver = new PathMatchingResourcePatternResolver();
        // 1) classpath:skills/*/SKILL.md
        try {
            Resource[] cpResources = resolver.getResources("classpath*:skills/*/SKILL.md");
            for (Resource r : cpResources) loadOne(r);
        } catch (Exception e) {
            log.debug("classpath skills not found: {}", e.getMessage());
        }
        // 2) file:./skills/*/SKILL.md (项目根)
        try {
            Resource[] fileResources = resolver.getResources("file:./skills/*/SKILL.md");
            for (Resource r : fileResources) loadOne(r);
        } catch (Exception ignored) {}
        log.info("SkillLoader loaded {} skills: {}", skills.size(), skills.keySet());
    }

    @SuppressWarnings("unchecked")
    private void loadOne(Resource r) {
        try (InputStream in = r.getInputStream()) {
            String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            if (!content.startsWith("---")) {
                log.warn("Skill file has no frontmatter: {}", r);
                return;
            }
            int end = content.indexOf("\n---", 3);
            if (end < 0) return;
            String fmText = content.substring(3, end).trim();
            String body = content.substring(end + 4).trim();
            Map<String, Object> fm = yaml.readValue(fmText, Map.class);
            String name = String.valueOf(fm.getOrDefault("name", r.getFilename()));
            String desc = String.valueOf(fm.getOrDefault("description", ""));
            List<String> triggers = (List<String>) fm.getOrDefault("triggers", List.of());
            List<String> tools = (List<String>) fm.getOrDefault("tools", List.of());
            skills.put(name, new Skill(name, desc, triggers, tools, body));
            log.info("Loaded skill [{}] from {}", name, r);
        } catch (Exception e) {
            log.warn("load skill failed {}: {}", r, e.getMessage());
        }
    }
}
