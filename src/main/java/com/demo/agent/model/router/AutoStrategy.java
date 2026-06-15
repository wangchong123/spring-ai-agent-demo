package com.demo.agent.model.router;

import com.demo.agent.config.AgentProperties;
import com.demo.agent.orchestrator.Complexity;
import com.demo.agent.orchestrator.Intent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 简单的规则路由：解析 application.yml 里的 routing.rules 表达式。
 * 表达式支持：complexity == X / intent == X / tokenEstimate > N
 */
@Component
@RequiredArgsConstructor
public class AutoStrategy implements RoutingStrategy {

    private final AgentProperties props;

    private static final Pattern P = Pattern.compile(
            "(complexity|intent|tokenEstimate)\\s*(==|>|<|>=|<=)\\s*(\\w+)");

    @Override
    public String resolve(RoutingContext ctx) {
        for (AgentProperties.RoutingRule rule : props.getRouting().getRules()) {
            if (matches(rule.getWhen(), ctx)) {
                return rule.getModel();
            }
        }
        return props.getRouting().getDefault();
    }

    private boolean matches(String expr, RoutingContext ctx) {
        if (expr == null || expr.isBlank()) return false;
        Matcher m = P.matcher(expr);
        if (!m.find()) return false;
        String var = m.group(1);
        String op = m.group(2);
        String val = m.group(3);
        return switch (var) {
            case "complexity" -> ctx.complexity() != null
                    && "==".equals(op)
                    && ctx.complexity() == safeComplexity(val);
            case "intent" -> ctx.intent() != null
                    && "==".equals(op)
                    && ctx.intent() == safeIntent(val);
            case "tokenEstimate" -> compareInt(ctx.tokenEstimate(), op, parseInt(val));
            default -> false;
        };
    }

    private Complexity safeComplexity(String v) {
        try { return Complexity.valueOf(v); } catch (Exception e) { return null; }
    }
    private Intent safeIntent(String v) {
        try { return Intent.valueOf(v); } catch (Exception e) { return null; }
    }
    private int parseInt(String v) { try { return Integer.parseInt(v); } catch (Exception e) { return 0; } }
    private boolean compareInt(int a, String op, int b) {
        return switch (op) {
            case ">" -> a > b;
            case "<" -> a < b;
            case ">=" -> a >= b;
            case "<=" -> a <= b;
            case "==" -> a == b;
            default -> false;
        };
    }
}
