package com.demo.agent.orchestrator;

import java.util.*;

public class TaskDag {

    private final Map<String, SubTask> tasks = new LinkedHashMap<>();
    private final Map<String, List<String>> children = new LinkedHashMap<>();

    public TaskDag(List<SubTask> subTasks) {
        for (SubTask t : subTasks) tasks.put(t.id(), t);
        for (SubTask t : subTasks) {
            for (String dep : t.dependsOn()) {
                children.computeIfAbsent(dep, k -> new ArrayList<>()).add(t.id());
            }
        }
    }

    public List<List<SubTask>> topoLayers() {
        Map<String, Integer> indeg = new HashMap<>();
        for (SubTask t : tasks.values()) indeg.put(t.id(), t.dependsOn().size());
        List<List<SubTask>> layers = new ArrayList<>();
        while (!indeg.isEmpty()) {
            List<SubTask> layer = new ArrayList<>();
            for (Map.Entry<String, Integer> e : indeg.entrySet())
                if (e.getValue() == 0) layer.add(tasks.get(e.getKey()));
            if (layer.isEmpty()) throw new IllegalStateException("DAG has a cycle");
            layers.add(layer);
            for (SubTask t : layer) {
                indeg.remove(t.id());
                for (String c : children.getOrDefault(t.id(), List.of())) {
                    indeg.computeIfPresent(c, (k, v) -> v - 1);
                }
            }
        }
        return layers;
    }
}
