package com.demo.agent.rag;

import java.util.Map;

public record TextChunk(String content, Map<String, Object> metadata) {}
