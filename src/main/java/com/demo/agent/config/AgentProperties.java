package com.demo.agent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@ConfigurationProperties(prefix = "agent")
public class AgentProperties {

    private boolean mockWhenNoApiKey = true;
    private Map<String, ModelConfig> models = new LinkedHashMap<>();
    private Routing routing = new Routing();
    private Memory memory = new Memory();
    private A2A a2a = new A2A();

    @Data
    public static class ModelConfig {
        private String provider;
        private String apiKey;
        private String baseUrl;
        private String model;
        private double inputPrice;
        private double outputPrice;
        private int contextWindow;
    }

    @Data
    public static class Routing {
        private String defaultModel = "deepseek-flash";
        private List<RoutingRule> rules = List.of();

        public String getDefault() { return defaultModel; }
        public void setDefault(String d) { this.defaultModel = d; }
    }

    @Data
    public static class RoutingRule {
        private String when;
        private String model;
    }

    @Data
    public static class Memory {
        private int l1MaxMessages = 20;
        private int compressBatchSize = 10;
    }

    @Data
    public static class A2A {
        private boolean enabled = true;
        private int selfPort = 8080;
        private List<String> peers = List.of();
    }
}
