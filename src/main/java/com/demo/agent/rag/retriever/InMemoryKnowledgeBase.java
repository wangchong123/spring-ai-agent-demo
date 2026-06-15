package com.demo.agent.rag.retriever;

import com.demo.agent.rag.TextChunk;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 内存知识库（演示用）。把 ingest 的 chunk 保存在内存，提供两路简化检索：
 *   - 朴素关键词召回（替代 Vector 路）
 *   - BM25 近似（替代 BM25 路）
 * 真实生产请替换为 PgVector / Elasticsearch。
 */
@Slf4j
@Component
public class InMemoryKnowledgeBase {

    private final Map<String, TextChunk> store = new ConcurrentHashMap<>();
    private final AtomicLong seq = new AtomicLong();

    public List<String> ingest(List<TextChunk> chunks) {
        List<String> ids = new ArrayList<>();
        for (TextChunk c : chunks) {
            String id = "doc-" + seq.incrementAndGet();
            store.put(id, c);
            ids.add(id);
        }
        log.info("ingested {} chunks, total {}", chunks.size(), store.size());
        return ids;
    }

    public Map<String, TextChunk> all() { return Collections.unmodifiableMap(store); }

    public int size() { return store.size(); }

    public void clear() { store.clear(); seq.set(0); }
}
