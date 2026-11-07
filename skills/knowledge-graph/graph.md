# Winter-Cloud 知识图谱明细

## 1. 模块依赖

`mermaid
flowchart TB
  subgraph ui [Frontend]
    VUE[winter-cloud-ui]
  end
  subgraph services [Services]
    GW[winter-cloud-gateway]
    AUTH[winter-cloud-auth]
    SYS[winter-cloud-system]
    DS[winter-cloud-datasource]
    XJ[winter-cloud-xxljob]
    FILE[winter-cloud-file]
  end
  subgraph api [API 模块]
    APIX[winter-cloud-api-xxljob]
  end
  subgraph infra [基础设施]
    NACOS[Nacos 8848]
    MYSQL[(MySQL 13306/winter-cloud)]
    REDIS[(Redis 6379)]
  end
  VUE --> GW
  GW --> AUTH
  GW --> SYS
  GW --> DS
  GW --> XJ
  GW --> FILE
  DS --> APIX
  APIX -->|Feign| XJ
  DS --> MYSQL
  XJ --> MYSQL
  DS --> REDIS
  AUTH --> REDIS
  SYS --> REDIS
  DS --> NACOS
  XJ --> NACOS
`

## 2. 数据源域模型

`mermaid
erDiagram
  DS_JDBC_DATASOURCE ||--o{ DS_JOB_INFO : uses
  DS_API_DATASOURCE ||--o| DS_API_DATASOURCE : relies_on
  DS_JOB_INFO ||--o{ DS_JOB_LOG : produces
  DS_JOB_INFO ||--o{ DS_JOB_STATISTICS : aggregates
  DS_JOB_INFO }o--|| XXL_JOB_INFO : binds_xxlJobId

  DS_API_DATASOURCE {
    bigint id PK
    string datasource_name
    string datasource_group
    string api_url
    int req_type
    string data_item_key
    text req_params_json
    bigint rely_datasource_id FK
    int belong_to_rely_datasource
    int status
  }
`

### API 数据源要点

- 控制器：DsApiDatasourceController，前缀 /api
- 依赖候选：GET /api/rely/list（勿用易与 /{id} 冲突的路径）
- 依赖出参：GET /api/fields/{id}，前端填 {{rely.xxx}}
- 测试连接会解析依赖并替换占位符

### 作业要点

- 表：ds_job_info / ds_job_log / ds_job_statistics
- 执行器 Handler：datasourceJobHandler
- 保存作业时 Feign 同步 XXL，回写 xxlJobId

## 3. XXL-JOB 链路

`mermaid
sequenceDiagram
  participant DS as datasource 执行器
  participant XJ as xxljob Admin
  DS->>XJ: POST /api/registry (Header XXL-JOB-ACCESS-TOKEN)
  alt token 一致
    XJ-->>DS: code=200
  else token 不一致
    XJ-->>DS: code=500 The access token is wrong
  end
  Note over DS,XJ: accessToken 现统一 default_token
  Note over DS: AppName=datasource-data-extra port=9998
  Note over DS: admin.addresses=http://127.0.0.1:9205
`

相关类：

- 执行器配置：datasource/config/XxlJobConfig.java
- 调度校验：xxljob/controller/JobApiController.java
- 内部 Feign：XxlJobInfoInnerController / XxlJobGroupInnerController

## 4. 配置锚点

| 项 | 位置 |
|----|------|
| Redis | 各 winter-*-dev.yml → spring.redis |
| Datasource 执行器 | winter-datasource-dev.yml → xxl.job.* |
| 调度中心 token | winter-xxljob-dev.yml → xxl.job.accessToken |
| 网关路由 | winter-gateway-dev.yml → /datasource/** StripPrefix=1 |
| 菜单 JDBC 名 | sys_menu.menu_id=2101 → JDBC数据源 |

## 5. 前端页面

| 路径 | 视图 |
|------|------|
| datasource/info | JDBC 数据源 |
| datasource/api | API 数据源 |
| datasource/job | 作业信息 |
| datasource/jobLog | 作业日志 |
| datasource/jobStats | 作业统计 |

API：winter-cloud-ui/src/api/datasource/*.js
