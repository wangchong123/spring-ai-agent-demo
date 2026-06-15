package com.demo.agent.rag.retriever;

import com.demo.agent.rag.TextChunk;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 朴素长度匹配召回（演示版第二路），用于演示多路召回 + RRF 融合。
 * 思想：偏爱中等长度且包含 query 子串的 chunk。
 */
@Component
@RequiredArgsConstructor
public class SubstringRetriever implements Retriever {

    private final InMemoryKnowledgeBase kb;

    @Override public String name() { return "substring"; }

    @Override
    public List<RetrievedDoc> retrieve(String query, int topK) {
        if (query == null || query.isBlank()) return List.of();
        List<RetrievedDoc> docs = new ArrayList<>();
        for (Map.Entry<String, TextChunk> e : kb.all().entrySet()) {
            int hits = countSubstr(e.getValue().content(), query);
            if (hits > 0) {
                double score = hits * 1.0 / Math.max(50, e.getValue().content().length());
                docs.add(new RetrievedDoc(e.getKey(), e.getValue().content(), score,
                        e.getValue().metadata()));
            }
        }
        docs.sort((a, b) -> Double.compare(b.score(), a.score()));
        return docs.subList(0, Math.min(topK, docs.size()));
    }

    private int countSubstr(String haystack, String needle) {
        if (haystack == null || needle == null) return 0;
        int hits = 0, idx = 0;
        // 用滑动窗口数子串：把 query 拆 2-gram 看命中
        for (int i = 0; i + 2 <= needle.length(); i++) {
            String n2 = needle.substring(i, i + 2);
            if (haystack.contains(n2)) hits++;
        }
        return hits;
    }
}
