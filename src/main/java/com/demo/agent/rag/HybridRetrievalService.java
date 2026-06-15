package com.demo.agent.rag;

import com.demo.agent.rag.fusion.RRFFusion;
import com.demo.agent.rag.retriever.RetrievedDoc;
import com.demo.agent.rag.retriever.Retriever;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.StructuredTaskScope;

/**
 * 多路召回 + RRF 融合 + （此处省略 reranker，演示版用 RRF score 直接当 final score）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HybridRetrievalService {

    private final List<Retriever> retrievers;
    private final RRFFusion rrf;

    @SuppressWarnings("preview")
    public List<RetrievedDoc> retrieve(String query, int topK) {
        if (retrievers.isEmpty()) return List.of();
        Map<String, List<RetrievedDoc>> rankings = new HashMap<>();
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            Map<String, StructuredTaskScope.Subtask<List<RetrievedDoc>>> subs = new HashMap<>();
            for (Retriever r : retrievers) {
                subs.put(r.name(), scope.fork(() -> r.retrieve(query, Math.max(20, topK * 4))));
            }
            scope.join();
            scope.throwIfFailed();
            subs.forEach((n, s) -> rankings.put(n, s.get()));
        } catch (Exception e) {
            log.warn("hybrid retrieve failed: {}", e.getMessage());
            return List.of();
        }
        return rrf.fuse(rankings, topK);
    }
}
