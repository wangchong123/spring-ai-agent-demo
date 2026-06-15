package com.demo.agent.skill;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/skills")
@RequiredArgsConstructor
@CrossOrigin
public class SkillController {

    private final SkillLoader loader;

    @GetMapping
    public Map<String, Object> list() {
        return Map.of("count", loader.getSkills().size(),
                "skills", loader.getSkills());
    }
}
