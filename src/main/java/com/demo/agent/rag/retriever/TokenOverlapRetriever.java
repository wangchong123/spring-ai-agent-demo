package com.demo.agent.rag.retriever;

import com.demo.agent.rag.TextChunk;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 朴素 token-overlap 召回（演示版）。topK 按 query 与 chunk 字符 2-gram 重合度排序。
 * 真实生产请用 Vector / BM25。
 */
@Component
@RequiredArgsConstructor
public class TokenOverlapRetriever implements Retriever {

    private final InMemoryKnowledgeBase kb;

    @Override public String name() { return "token-overlap"; }

    @Override
    public List<RetrievedDoc> retrieve(String query, int topK) {
        if (query == null || query.isBlank()) return List.of();
        Set<String> qTokens = bigrams(query);
        List<RetrievedDoc> docs = new ArrayList<>();
        for (Map.Entry<String, TextChunk> e : kb.all().entrySet()) {
            Set<String> dTokens = bigrams(e.getValue().content());
            if (dTokens.isEmpty()) continue;
            int overlap = 0;
            for (String t : qTokens) if (dTokens.contains(t)) overlap++;
            double score = (double) overlap / Math.max(1, qTokens.size());
            if (score > 0) {
                docs.add(new RetrievedDoc(e.getKey(), e.getValue().content(), score,
                        e.getValue().metadata()));
            }
        }
        docs.sort((a, b) -> Double.compare(b.score(), a.score()));
        return docs.subList(0, Math.min(topK, docs.size()));
    }

    private Set<String> bigrams(String s) {
        Set<String> out = new HashSet<>();
        if (s == null) return out;
        for (int i = 0; i + 2 <= s.length(); i++) out.add(s.substring(i, i + 2));
        return out;
    }
}
