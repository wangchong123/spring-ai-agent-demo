package com.demo.agent.tool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 演示业务工具：财务查询。
 * 真实场景接 Wind / 同花顺 / Tushare / 公司内部财务 API。
 */
@Slf4j
@Component
public class FinanceTools {

    private static final Map<String, Map<String, Object>> MOCK_DATA = new LinkedHashMap<>();
    static {
        MOCK_DATA.put("BABA-2025Q4", Map.of(
                "ticker", "BABA", "quarter", "2025Q4",
                "revenue", "2,365 亿元", "netIncome", "434 亿元",
                "grossMargin", "39.2%", "operatingCashFlow", "412 亿元"));
        MOCK_DATA.put("0700-2025Q4", Map.of(
                "ticker", "0700.HK", "quarter", "2025Q4",
                "revenue", "1,672 亿元", "netIncome", "514 亿元",
                "grossMargin", "53.8%", "operatingCashFlow", "667 亿元"));
    }

    @Tool(description = "查询某公司某季度财报关键指标，返回营收/净利润/毛利率/经营现金流。" +
            "ticker 例如 BABA / 0700。quarter 例如 2025Q4。")
    public Map<String, Object> queryReport(
            @ToolParam(description = "股票代码，如 BABA / 0700") String ticker,
            @ToolParam(description = "季度，如 2025Q4") String quarter) {
        log.info("[tool] queryReport ticker={} quarter={}", ticker, quarter);
        Map<String, Object> data = MOCK_DATA.get(ticker.toUpperCase() + "-" + quarter.toUpperCase());
        if (data == null) {
            return Map.of("error", "no data", "ticker", ticker, "quarter", quarter);
        }
        return data;
    }

    @Tool(description = "对比两家公司同期财务关键指标")
    public Map<String, Object> compareReport(
            @ToolParam(description = "公司A 股票代码") String tickerA,
            @ToolParam(description = "公司B 股票代码") String tickerB,
            @ToolParam(description = "季度，如 2025Q4") String quarter) {
        Map<String, Object> a = queryReport(tickerA, quarter);
        Map<String, Object> b = queryReport(tickerB, quarter);
        return Map.of(
                "quarter", quarter,
                "companyA", a,
                "companyB", b,
                "summary", "对比 " + tickerA + " 与 " + tickerB + " 的营收、净利与毛利率，详见上述结构化数据");
    }
}
