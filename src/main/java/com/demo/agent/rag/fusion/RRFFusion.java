package com.demo.agent.rag.fusion;

import com.demo.agent.rag.retriever.RetrievedDoc;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Reciprocal Rank Fusion（标准实现）。score(d) = sum 1/(K+rank_i)
 */
@Component
public class RRFFusion {

    private static final int K = 60;

    public List<RetrievedDoc> fuse(Map<String, List<RetrievedDoc>> rankings, int topK) {
        Map<String, Double> scoreMap = new HashMap<>();
        Map<String, RetrievedDoc> docMap = new HashMap<>();
        for (List<RetrievedDoc> docs : rankings.values()) {
            for (int rank = 0; rank < docs.size(); rank++) {
                RetrievedDoc d = docs.get(rank);
                scoreMap.merge(d.id(), 1.0 / (K + rank + 1), Double::sum);
                docMap.putIfAbsent(d.id(), d);
            }
        }
        return scoreMap.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(topK)
                .map(e -> {
                    RetrievedDoc d = docMap.get(e.getKey());
                    return new RetrievedDoc(d.id(), d.content(), e.getValue(), d.metadata());
                })
                .toList();
    }
}
