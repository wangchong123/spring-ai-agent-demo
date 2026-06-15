package com.demo.agent.tool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
public class CommonTools {

    @Tool(description = "获取当前北京时间")
    public String currentTime() {
        return ZonedDateTime.now(ZoneId.of("Asia/Shanghai"))
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    @Tool(description = "在互联网上搜索信息（演示版本，返回模拟结果）")
    public String webSearch(@ToolParam(description = "搜索关键词") String query) {
        log.info("[tool] webSearch query={}", query);
        return "[搜索结果] 关于 \"" + query + "\" 的前 3 条摘要：\n"
                + "1) 这是一条演示用的搜索结果\n"
                + "2) 接入真实 SearXNG / Tavily / Bing 即可使用真实数据\n"
                + "3) 在 application.yml 配置 search.provider";
    }

    @Tool(description = "做加法：返回 a + b")
    public double add(@ToolParam(description = "数值 a") double a,
                      @ToolParam(description = "数值 b") double b) {
        return a + b;
    }
}
