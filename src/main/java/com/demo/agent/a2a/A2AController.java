package com.demo.agent.a2a;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/a2a")
@RequiredArgsConstructor
@CrossOrigin
public class A2AController {

    private final A2ARegistry registry;

    @GetMapping("/peers")
    public Map<String, Object> peers() {
        return Map.of("count", registry.all().size(), "peers", registry.all());
    }
}
