---
name: pdf-analyze
description: 分析 PDF / 文档，提取关键财务数据、关键事件，生成结构化摘要
triggers:
  - "分析这份"
  - "分析 PDF"
  - "提取财报"
  - "总结报告"
tools:
  - queryReport
  - compareReport
  - webSearch
---

# pdf-analyze 技能

## 使用步骤
1. 识别报告类型（年报 / 季报 / 招股书 / 第三方研报）
2. 提取财务三表关键数据：营收、净利、毛利率、经营现金流
3. 必要时调用 queryReport / compareReport 拉取历史或同业数据做对比
4. 输出结构化结果，含：summary / keyMetrics[] / risks[] / recommendations[]
