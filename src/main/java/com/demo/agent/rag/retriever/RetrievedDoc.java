package com.demo.agent.rag.retriever;

import java.util.Map;

public record RetrievedDoc(String id, String content, double score, Map<String, Object> metadata) {}
