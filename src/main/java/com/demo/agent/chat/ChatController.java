package com.demo.agent.chat;

import com.demo.agent.memory.LayeredChatMemory;
import com.demo.agent.model.ModelRegistry;
import com.demo.agent.model.router.SessionStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@CrossOrigin
public class ChatController {

    private final ChatService chatService;
    private final ModelRegistry registry;
    private final SessionStrategy sessionStrategy;
    private final LayeredChatMemory memory;

    @PostMapping
    public SimpleChatResponse chat(@RequestBody ChatRequest req) {
        return chatService.chat(req);
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> stream(@RequestBody ChatRequest req) {
        return chatService.stream(req)
                .map(token -> ServerSentEvent.<String>builder().event("token").data(token).build())
                .concatWith(Mono.just(ServerSentEvent.<String>builder().event("done").data("[DONE]").build()));
    }

    @GetMapping("/models")
    public Map<String, Object> models() {
        return Map.of("models", registry.all());
    }

    @PostMapping("/session/{sessionId}/model")
    public Map<String, Object> bindSessionModel(@PathVariable String sessionId,
                                                @RequestParam String model) {
        if (!registry.keys().contains(model)) {
            return Map.of("ok", false, "msg", "unknown model: " + model);
        }
        sessionStrategy.bind(sessionId, model);
        return Map.of("ok", true, "sessionId", sessionId, "model", model);
    }

    @DeleteMapping("/session/{sessionId}")
    public Map<String, Object> clear(@PathVariable String sessionId) {
        memory.clear(sessionId);
        sessionStrategy.clear(sessionId);
        return Map.of("ok", true, "sessionId", sessionId);
    }
}
