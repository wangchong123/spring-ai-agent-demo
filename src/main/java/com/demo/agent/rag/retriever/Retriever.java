package com.demo.agent.rag.retriever;

import java.util.List;

public interface Retriever {
    String name();
    List<RetrievedDoc> retrieve(String query, int topK);
}
