package com.demo.agent.model.router;

public interface RoutingStrategy {
    String resolve(RoutingContext ctx);
}
