---
name: sql-query
description: 自然语言生成 SQL 并演示执行；演示版仅打印 SQL，不连接真实数据库
triggers:
  - "用 SQL 查"
  - "生成 SQL"
  - "查询表"
tools:
  - currentTime
---

# sql-query 技能

## 使用步骤
1. 识别用户问题里的实体、时间窗口、聚合维度
2. 生成可执行的 SQL（兼容 PostgreSQL）
3. 输出：SQL 语句、字段说明、预期结果格式
