package com.demo.agent.skill;

import java.util.List;

public record Skill(
        String name,
        String description,
        List<String> triggers,
        List<String> tools,
        String prompt
) {}
