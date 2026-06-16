package com.demo.agent.rag;

import com.demo.agent.rag.chunker.Chunker;
import com.demo.agent.rag.retriever.InMemoryKnowledgeBase;
import com.demo.agent.rag.retriever.RetrievedDoc;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rag")
@RequiredArgsConstructor
@CrossOrigin
public class RagController {

    private final InMemoryKnowledgeBase kb;
    private final HybridRetrievalService retrieval;

    @Qualifier("recursive")
    private final Chunker recursive;

    @PostMapping("/ingest")
    public Map<String, Object> ingest(@RequestBody IngestRequest req) {
        String docId = req.docId() == null ? "doc-" + System.currentTimeMillis() : req.docId();
        int target = (req.targetSize() == null || req.targetSize() <= 0) ? 400 : req.targetSize();
        int overlap = (req.overlap() == null || req.overlap() < 0) ? 50 : req.overlap();
        List<TextChunk> chunks = recursive.split(req.content(),
                new ChunkContext(target, overlap, docId));
        List<String> ids = kb.ingest(chunks);
        return Map.of("ok", true, "docId", docId, "chunks", chunks.size(), "ids", ids);
    }

    @GetMapping("/search")
    public Map<String, Object> search(@RequestParam String q,
                                      @RequestParam(defaultValue = "5") int topK) {
        List<RetrievedDoc> docs = retrieval.retrieve(q, topK);
        return Map.of("query", q, "topK", topK, "results", docs);
    }

    @DeleteMapping
    public Map<String, Object> clear() {
        kb.clear();
        return Map.of("ok", true);
    }

    public record IngestRequest(String docId, String content, Integer targetSize, Integer overlap) {}
}
