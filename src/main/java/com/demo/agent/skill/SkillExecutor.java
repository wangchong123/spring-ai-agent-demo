package com.demo.agent.skill;

import com.demo.agent.chat.ChatClientFactory;
import com.demo.agent.tool.ToolRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
@RequiredArgsConstructor
public class SkillExecutor {

    private final ChatClientFactory factory;
    private final ToolRegistry toolRegistry;

    public Flux<String> stream(Skill skill, String userMessage, String modelKey, String sessionId) {
        return factory.client(modelKey).prompt()
                .system(skill.prompt())
                .user(userMessage)
                .toolCallbacks(toolRegistry.pick(skill.tools()))
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, sessionId))
                .stream().content();
    }
}
