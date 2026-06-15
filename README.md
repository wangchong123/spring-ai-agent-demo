# Spring AI Agent Demo

> 一个生产级的 Java AI Agent 工程示例，配套公众号三篇连载文章。
>
> 涵盖：**多模型路由 · 三层记忆 · 流式对话 · RAG（多路召回 + RRF 融合）· 工具调用 · Skill 系统 · 意图识别 · SubAgent DAG 编排 · A2A 跨进程协作**。

## ✨ 特性

- **多模型并存**：DeepSeek / 通义千问 / 智谱 GLM 通过 OpenAI 兼容协议统一接入，一个工程切换三家
- **智能路由**：手动 / 会话 / 自动三种策略，按意图与复杂度动态选模型；主模型失败自动降级
- **三层记忆**：L1 工作记忆（Redis List）+ L2 滚动摘要 + L3 长期事实，超阈值自动压缩
- **完整 RAG 链路**：3 种 Chunker（fixed / recursive / markdown）+ 多路召回 + RRF 融合
- **工具调用**：基于 Spring AI `@Tool` 注解，自动收集为 `ToolRegistry`，可按 Skill 白名单按需暴露
- **Skill 系统**：扫描 `skills/*/SKILL.md`，按触发词命中后注入 Prompt + 工具子集
- **意图识别 + DAG 编排**：复杂任务自动拆 SubAgent，按 DAG 拓扑分层并发（JDK 21 结构化并发）
- **A2A 协议**：每个实例自带 `/.well-known/agent.json`，支持 `peer` 互联
- **Mock 模式**：未配置 API Key 时自动启用 mock 模型，骨架可直接 `mvn spring-boot:run`

## 🚀 快速开始

### 前置依赖
- JDK 21+
- Maven 3.8+
- Redis 7（推荐 docker 跑）；不启 Redis 的话部分记忆功能会降级

### 一、启动依赖

```bash
docker compose up -d redis
# 想体验完整 PgVector 也可以：
docker compose up -d
```

### 二、配置 API Key（可选）

```bash
cp .env.example .env
# 编辑 .env 填入你的 DEEPSEEK_API_KEY / DASHSCOPE_API_KEY / ZHIPU_API_KEY
# 不填也能跑，会启用 mock 模式
export $(cat .env | xargs)
```

### 三、运行

```bash
mvn spring-boot:run
```

访问 http://localhost:8080 即可看到内置演示页面。

## 🧪 接口示例

### 列出已注册模型
```bash
curl http://localhost:8080/api/chat/models | jq
```

### 流式对话（自动路由）
```bash
curl -N -X POST http://localhost:8080/api/chat/stream \
  -H 'Content-Type: application/json' \
  -d '{"sessionId":"u1","message":"你好","model":"auto"}'
```

### 手动指定模型
```bash
curl -N -X POST http://localhost:8080/api/chat/stream \
  -H 'Content-Type: application/json' \
  -d '{"sessionId":"u1","message":"用 200 字解释 RAFT","model":"glm-flash"}'
```

### 触发复杂任务（DAG 拆解 + SubAgent）
```bash
curl -N -X POST http://localhost:8080/api/chat/stream \
  -H 'Content-Type: application/json' \
  -d '{"sessionId":"u1","model":"auto","message":"帮我分析这份阿里 2025Q4 季报，对比腾讯同期，并起草投资简报邮件"}'
```

### RAG 知识库
```bash
# 入库
curl -X POST http://localhost:8080/api/rag/ingest \
  -H 'Content-Type: application/json' \
  -d '{"docId":"qa","content":"# 阿里财报\n阿里巴巴 2025Q4 营收 2365 亿元，净利润 434 亿元，毛利率 39.2%。\n# 腾讯财报\n腾讯 2025Q4 营收 1672 亿元，净利润 514 亿元，毛利率 53.8%。"}'

# 检索（多路召回 + RRF）
curl 'http://localhost:8080/api/rag/search?q=阿里财报关键指标&topK=3' | jq
```

### 列出工具 / Skill / A2A peer
```bash
curl http://localhost:8080/api/tools | jq
curl http://localhost:8080/api/skills | jq
curl http://localhost:8080/api/a2a/peers | jq
curl http://localhost:8080/.well-known/agent.json | jq
```

### 会话级模型绑定
```bash
curl -X POST 'http://localhost:8080/api/chat/session/u1/model?model=qwen-plus'
# 之后该 session 的所有请求若不指定 model 字段，都走 qwen-plus
```

## 🗂 项目结构

```
spring-ai-agent-demo/
├── docker-compose.yml          # Redis + Postgres+pgvector
├── pom.xml
├── skills/                     # Skill 目录（运行时扫描）
│   ├── pdf-analyze/SKILL.md
│   └── sql-query/SKILL.md
└── src/main/java/com/demo/agent/
    ├── AgentApplication.java
    ├── config/                 # AgentProperties
    ├── chat/                   # ChatController / ChatService / ChatClientFactory
    ├── memory/                 # 三层记忆 + 压缩
    ├── model/                  # ModelRegistry + 路由（manual/session/auto）+ 成本统计
    ├── mock/                   # 无 API Key 时的兜底 ChatModel
    ├── rag/                    # 6 类 Chunker（已实现 3 种）+ 多路召回 + RRF
    ├── tool/                   # @Tool 注解工具 + 注册表
    ├── skill/                  # Skill 加载/路由/执行
    ├── orchestrator/           # 意图识别 + DAG + SubAgent + Reducer
    └── a2a/                    # A2A 协议 Server / Client / Registry
```

## 🔧 路由规则

`application.yml` 里可声明：

```yaml
agent:
  routing:
    default: deepseek-flash
    rules:
      - when: complexity == SIMPLE
        model: glm-flash
      - when: complexity == COMPLEX
        model: deepseek-pro
      - when: tokenEstimate > 200000
        model: qwen-plus
```

## 🛠 配套文章

- 📘 第一篇：基础对话与模型路由
- 📗 第二篇：RAG · 工具 · MCP · Skill
- 📕 第三篇：意图识别 · SubAgent 编排 · A2A · 生产化

## 🧭 Roadmap（阶段 B 增量补完）

- [ ] PgVector 真实 Embedding 索引
- [ ] BM25 全文检索（Postgres tsvector + ts_rank_cd）
- [ ] HyDE 假设答案召回
- [ ] DashScope Reranker
- [ ] Spring AI MCP Client / Server starter 双写
- [ ] Guardrails Advisor
- [ ] 语义缓存
- [ ] LLM-as-Judge 评测套件
- [ ] 独立 PDF / Mail Agent 子模块（A2A 真实跨进程）
- [ ] Grafana Dashboard

## 📜 License

MIT
