package com.demo.agent.rag.chunker;

import com.demo.agent.rag.ChunkContext;
import com.demo.agent.rag.TextChunk;

import java.util.List;

public interface Chunker {
    String name();
    List<TextChunk> split(String text, ChunkContext ctx);
}
