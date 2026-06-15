package com.demo.agent.rag.chunker;

import com.demo.agent.rag.ChunkContext;
import com.demo.agent.rag.TextChunk;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component("fixed")
public class FixedSizeChunker implements Chunker {
    @Override public String name() { return "fixed"; }

    @Override
    public List<TextChunk> split(String text, ChunkContext ctx) {
        List<TextChunk> out = new ArrayList<>();
        if (text == null || text.isEmpty()) return out;
        int step = Math.max(1, ctx.targetSize() - ctx.overlap());
        for (int i = 0; i < text.length(); i += step) {
            int end = Math.min(i + ctx.targetSize(), text.length());
            out.add(new TextChunk(text.substring(i, end),
                    Map.of("docId", ctx.docId() == null ? "" : ctx.docId(),
                           "offset", i, "chunker", name())));
            if (end == text.length()) break;
        }
        return out;
    }
}
