package com.demo.agent.rag;

public record ChunkContext(int targetSize, int overlap, String docId) {}
