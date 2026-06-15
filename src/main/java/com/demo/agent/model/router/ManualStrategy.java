package com.demo.agent.model.router;

import org.springframework.stereotype.Component;

@Component
public class ManualStrategy implements RoutingStrategy {
    @Override
    public String resolve(RoutingContext ctx) {
        return ctx.userPreferredModel();
    }
}
