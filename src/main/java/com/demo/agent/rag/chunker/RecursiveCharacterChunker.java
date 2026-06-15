package com.demo.agent.rag.chunker;

import com.demo.agent.rag.ChunkContext;
import com.demo.agent.rag.TextChunk;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Component("recursive")
public class RecursiveCharacterChunker implements Chunker {

    private static final List<String> SEPARATORS =
            List.of("\n\n", "\n", "。", "！", "？", "；", "，", " ", "");

    @Override public String name() { return "recursive"; }

    @Override
    public List<TextChunk> split(String text, ChunkContext ctx) {
        List<TextChunk> out = new ArrayList<>();
        for (String s : splitRecursive(text, ctx, 0)) {
            out.add(new TextChunk(s, Map.of(
                    "docId", ctx.docId() == null ? "" : ctx.docId(),
                    "chunker", name())));
        }
        return out;
    }

    private List<String> splitRecursive(String text, ChunkContext ctx, int sepIdx) {
        if (text == null || text.isEmpty()) return List.of();
        if (text.length() <= ctx.targetSize()) return List.of(text);
        if (sepIdx >= SEPARATORS.size()) return forceSplit(text, ctx);

        String sep = SEPARATORS.get(sepIdx);
        String[] parts = sep.isEmpty() ? new String[]{text} : text.split(Pattern.quote(sep));
        List<String> result = new ArrayList<>();
        StringBuilder buf = new StringBuilder();
        for (String p : parts) {
            int prospective = buf.length() + p.length() + sep.length();
            if (prospective <= ctx.targetSize()) {
                if (!buf.isEmpty()) buf.append(sep);
                buf.append(p);
            } else {
                if (!buf.isEmpty()) {
                    result.add(buf.toString());
                    buf.setLength(0);
                }
                if (p.length() > ctx.targetSize()) {
                    result.addAll(splitRecursive(p, ctx, sepIdx + 1));
                } else {
                    buf.append(p);
                }
            }
        }
        if (!buf.isEmpty()) result.add(buf.toString());
        return result;
    }

    private List<String> forceSplit(String text, ChunkContext ctx) {
        List<String> out = new ArrayList<>();
        int step = Math.max(1, ctx.targetSize() - ctx.overlap());
        for (int i = 0; i < text.length(); i += step) {
            int end = Math.min(i + ctx.targetSize(), text.length());
            out.add(text.substring(i, end));
            if (end == text.length()) break;
        }
        return out;
    }
}
