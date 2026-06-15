package com.demo.agent.model;

public record ModelDescriptor(
        String key,
        String provider,
        String model,
        String baseUrl,
        int contextWindow,
        double inputPrice,
        double outputPrice,
        boolean mock
) {}
