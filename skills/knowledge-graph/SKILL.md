---
name: knowledge-graph
description: >-
  Winter-Cloud 项目知识图谱：模块关系、数据源/作业/XXL-JOB 链路、Nacos 配置与常见坑。
  在梳理架构、定位跨模块问题、改菜单/数据源/调度相关功能时使用。
---

# Winter-Cloud 知识图谱

先读本文件建立全局关系；细节按需打开 [graph.md](graph.md)。

## 何时使用

- 问模块依赖、调用链、表/菜单归属
- 改 JDBC/API 数据源、作业、XXL-JOB 注册
- 排查 Nacos/Redis/accessToken/雪花 ID 类问题

## 核心实体（速查）

| 实体 | 说明 |
|------|------|
| winter-cloud-gateway | 网关，路由 /datasource/** 等到各服务 |
| winter-cloud-datasource | JDBC/API 数据源、作业信息/日志/统计、XXL 执行器 |
| winter-cloud-xxljob | 内嵌调度中心，提供 /inner/jobinfo|jobgroup |
| winter-cloud-api-xxljob | Feign 客户端，datasource 调 xxljob |
| Nacos | winter-*-dev.yml，Redis/DB/xxl.job 配置源 |
| MySQL | winter-cloud @ 127.0.0.1:13306 |
| Redis | 本地 127.0.0.1:6379，无密码 |

## 关键关系（Mermaid）

`mermaid
flowchart LR
  UI[winter-cloud-ui] --> GW[gateway]
  GW --> DS[datasource]
  GW --> XJ[xxljob]
  DS -->|Feign api-xxljob| XJ
  DS -->|Executor registry| XJ
  DS --> DB[(MySQL winter-cloud)]
  XJ --> DB
  DS --> RD[(Redis)]
  XJ --> RD
  DS --> NC[Nacos winter-datasource-dev]
  XJ --> NX[Nacos winter-xxljob-dev]
`

## 硬约束（改代码前必看）

1. **雪花 Long**：JSON 必须字符串序列化（ToStringSerializer / 接口返回 String），否则前端丢精度。
2. **Spring 路径**：静态路径勿被 /{id} 吃掉；依赖候选用 /api/rely/list。
3. **XXL accessToken**：winter-xxljob 与 winter-datasource 的 xxl.job.accessToken 必须一致（现为 default_token）。
4. **执行器 AppName**：固定 datasource-data-extra，端口默认 9998。
5. **文件编码**：含中文源码/SQL/YAML 一律 UTF-8 无 BOM（见 .cursor/rules/utf8-encoding.mdc）。

## 菜单与表

| 菜单 | menu_id | 表/模块 |
|------|---------|---------|
| 数据源管理 | 2100 | 目录 |
| JDBC数据源 | 2101 | ds_jdbc_datasource |
| API数据源 | 2170 | ds_api_datasource |
| 作业信息/日志/统计 | 见 SQL | ds_job_* |

SQL 种子：sql/winter_datasource.sql；增量：sql/ds_api_datasource_alter.sql。
Nacos 模板：sql/winter_datasource_nacos.yml、sql/winter_xxljob_nacos.yml。

## 排查口诀

- 注册失败 ccess token is wrong → 对齐两边 xxl.job.accessToken 后重启执行器
- 依赖 API 下拉无数据 / 回显裸数字 → 查 /rely/list 与 ID 是否字符串
- 改 Nacos 本地文件后 → 需发布到 Nacos 并重启对应服务
