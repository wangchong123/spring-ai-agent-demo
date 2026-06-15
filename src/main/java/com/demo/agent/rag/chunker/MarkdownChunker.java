package com.demo.agent.rag.chunker;

import com.demo.agent.rag.ChunkContext;
import com.demo.agent.rag.TextChunk;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component("markdown")
public class MarkdownChunker implements Chunker {

    private static final Pattern HEADING = Pattern.compile("^(#{1,6})\\s+(.+)$", Pattern.MULTILINE);

    @Override public String name() { return "markdown"; }

    @Override
    public List<TextChunk> split(String text, ChunkContext ctx) {
        List<TextChunk> out = new ArrayList<>();
        if (text == null || text.isEmpty()) return out;
        Matcher m = HEADING.matcher(text);
        int last = 0;
        String currentHeading = "";
        while (m.find()) {
            if (last != 0 || m.start() > 0) {
                String chunk = text.substring(last, m.start()).trim();
                if (!chunk.isEmpty()) {
                    out.add(new TextChunk(chunk, Map.of(
                            "docId", ctx.docId() == null ? "" : ctx.docId(),
                            "heading", currentHeading,
                            "chunker", name())));
                }
            }
            currentHeading = m.group(2).trim();
            last = m.end();
        }
        if (last < text.length()) {
            String tail = text.substring(last).trim();
            if (!tail.isEmpty()) {
                out.add(new TextChunk(tail, Map.of(
                        "docId", ctx.docId() == null ? "" : ctx.docId(),
                        "heading", currentHeading, "chunker", name())));
            }
        }
        return out;
    }
}
