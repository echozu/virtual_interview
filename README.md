# 虚拟面试平台（Virtual Interview Backend）

一个面向求职者的智能化虚拟面试与学习平台后端，提供面试对话、语音识别与合成、AI 评估分析、知识库检索、内容社区等能力，帮助用户模拟真实面试、获得结构化反馈与学习资源。

### 功能亮点
- **账号与安全**：注册登录、JWT 鉴权、权限拦截、跨域与全局异常处理。
- **面试对话**：基于科大讯飞大模型的聊天与面试专家（`InterviewExpert`、`ChatExpert`），支持会话记忆与上下文管理（MySQL 持久化）。
- **语音能力**：
  - ASR 实时语音转写（Iflytek RTASR），WebSocket 流式处理，日志落盘到 `asr_audio_logs/`。
  - TTS 语音合成（Iflytek TTS），支持选择发音人，音频输出至 `tts_audio_logs/`。
- **视频与表情分析（可选）**：`flask-realtime-face-eye/` 子模块提供人脸与眼动/表情检测的实时分析服务，便于评估面试表现（姿态、表情、注视等）。
- **AI 评估与报告**：半分钟阶段性报告、回合分析、综合分析报告与学习资源推荐，支持图表与结构化指标输出。
- **知识库检索（RAG）**：集成 Spring AI 与讯飞知识库，支持向量检索、增强生成，提升答案的专业性与可解释性。
- **内容社区**：面经/帖子、评论、点赞收藏、排行榜、学习计划与资源管理。
- **文件上传与对象存储**：阿里云 OSS 集成，简化简历等文件的上传管理。
- **可观测性**：Actuator 端点、统一日志、全局异常返回结构。

### 技术栈
- **语言与运行环境**：Java 21、Maven、Spring Boot 3.4.x
- **Web 框架**：Spring Web MVC、Spring WebFlux（部分接口流式/响应式）
- **数据访问**：MyBatis-Plus、MyBatis、MySQL
- **缓存与消息**：Redis（Spring Cache）、Lettuce 连接池
- **安全**：JWT（jjwt）、拦截器鉴权、AOP 日志
- **文档与调试**：Springdoc OpenAPI（`/swagger-ui.html`、`/api-docs`）
- **AI 能力**：Spring AI、Iflytek 大模型（Chat/Knowledge）、RAG（向量检索、Advisors）
- **实时通信**：Spring WebSocket、Java-WebSocket（客户端场景）
- **对象存储**：阿里云 OSS SDK
- **工具**：Hutool、Jackson、Fastjson、Apache POI、OkHttp、Lombok
- **可观测**：Spring Boot Actuator

### AI 能力详解
- **对话与面试专家（LLM）**：基于科大讯飞 Spark（`pro-128k`），通过 Spring AI 封装的 `XunFeiChatModel`、`XunFeiConfig`、`XunFeiProperties` 与提示词集 `InterviewPrompts`，实现通用聊天（`ChatExpert`）与面试官角色（`InterviewExpert`）。支持流式输出（WebFlux/SSE 或 WebSocket）。
- **会话记忆（Chat Memory）**：`MysqlChatMemory`、`MysqlInterviewMemory` 将多轮消息持久化至 MySQL，结合用户上下文与历史对话提升连续性与一致性。
- **RAG 知识增强**：集成讯飞知识库与 Spring AI 向量检索（`spring-ai-advisors-vector-store`、`spring-ai-markdown-document-reader`），通过 `XfyunSparkKnowledgeClient`、`XfyunSparkKnowledgeRetriever`、`XfyunKnowledgeHelper` 等组件，在生成前进行检索与上下文增强，知识库名配置为 `interview`。
- **ASR 实时转写**：`utils/rtasr/IflytekAsrClient` 进行 WebSocket 流式语音转写（RTASR），请求/响应使用 `model/dto/interview/audio/*`，落盘日志在 `asr_audio_logs/`，便于追踪与复现。
- **TTS 语音合成**：`utils/tst/TtsService` 基于 Iflytek TTS 输出音频，参数由 `TtsRequest` 控制（发音人 `vcn` 等），产物写入 `tts_audio_logs/`，可用于语音播报与面试官语音化。
- **表情/姿态分析**：`utils/face/IflytekExpressionService`、`model/dto/interview/process/*`（如 `HeadPose`、`IflytekExpressionResponse`）结合 `flask-realtime-face-eye/` 子服务，支持面试过程中的表情、注视点与头部姿态评估。
- **流式通信与控制**：`WebSocketConfig`、`InterviewWsController` 提供面试过程中的实时通道，便于推送问题、接收答案、同步评估指标与控制面板。
- **评估与报告**：阶段性/回合性分析（`HalfMinuteReport`、`TurnAnalysisResponse`）与综合报告（`AnalysisReportDTO`），用于面试结束后的拆解与学习建议生成。

### 目录结构（关键部分）
```
virtual_interview/
├─ src/main/java/com/echo/virtual_interview/
│  ├─ controller/           # 账户、面试、聊天、简历、内容社区等接口
│  ├─ service/              # 业务逻辑与集成实现（ASR/TTS/分析/RAG/OSS）
│  ├─ model/                # DTO/VO/Entity/枚举
│  ├─ mapper/               # MyBatis-Plus Mapper 接口
│  ├─ config/               # 全局配置、拦截器、CORS、WebSocket、线程池等
│  ├─ aop/                  # 日志与鉴权切面/拦截器
│  └─ utils/                # JWT、ASR/TTS、表情分析等工具
├─ src/main/resources/
│  ├─ application.yml       # 全局配置（使用 prod/dev 外部化变量）
│  └─ mapper/               # MyBatis XML 映射
├─ flask-realtime-face-eye/ # 可选的表情/人脸/注视实时分析子服务（Flask）
├─ asr_audio_logs/          # 语音转写日志
├─ tts_audio_logs/          # 语音合成音频日志
└─ logs/                    # 应用运行日志
```

### 配置说明

- 在 `src/main/resources/application.yml` 中，应用名为 `virtual_interview`，默认 `spring.profiles.active=prod`。
- 数据源、Redis、OSS 与 Iflytek 等敏感配置通过占位符 `${echo.*}` 注入，请在实际运行环境中通过 `application-prod.yml` 或系统环境变量提供：
  - `echo.datasource.*`（host、port、database、username、password、driver-class-name）
  - `echo.redis.*`（host、port、database、password）
  - `echo.oss.*`（endpoint、access-key-id、access-key-secret、bucket-name）
  - `xunfei.*`（api/rtasr/tts/knowledge/face-score 等）
- JWT：`jwt.secret`、`jwt.expiration` 可按需调整。
- Swagger：`/swagger-ui.html`，接口包扫描 `com.echo.virtual_interview.controller`。

### 产品图片介绍

#### 数据库UML图

![图片1.png](https://img.remit.ee/api/file/BQACAgUAAyEGAASHRsPbAAECLIFozQ0HwE7IZI0nKWnPiGxUUAYg4gAC5BgAAhwQaVYxTCz2tzZgMDYE.png)

#### 登录界面

![图片2.png](https://img.remit.ee/api/file/BQACAgUAAyEGAASHRsPbAAECLIBozQ0CUn0i0Tf6JVzH2G7bbIv8BQAC4xgAAhwQaVbPkriNEnqyszYE.png)

#### 智能对话

![图片3.png](https://img.remit.ee/api/file/BQACAgUAAyEGAASHRsPbAAECLHxozQ0BQwW2I0R-qQTWGiK83dBCygAC3xgAAhwQaVbi8N4OAdN9uzYE.png)

#### 选择面试

![微信图片_20250919160351_855_4.png](https://img.remit.ee/api/file/BQACAgUAAyEGAASHRsPbAAECLKNozQ7Zlc0J3SK7OOLsCQ5ljTRIJAACBhkAAhwQaVZkI1LfzQ0AAa42BA.png)

#### 面试过程

![微信图片_20250919155653_854_4.png](https://img.remit.ee/api/file/BQACAgUAAyEGAASHRsPbAAECLIJozQ0H7fEZ5KTVbSpNAe_3n8y0lwAC5RgAAhwQaVZdsEul3xEeNTYE.png)

#### 分析报告

![图片4.png](https://img.remit.ee/api/file/BQACAgUAAyEGAASHRsPbAAECLH1ozQ0BiO1jW_NsoO7pCwGyPHjq4wAC4BgAAhwQaVZ8GWJLUaN1CjYE.png)

#### 发布面经

![图片5.png](https://img.remit.ee/api/file/BQACAgUAAyEGAASHRsPbAAECLKJozQ7Y2F-B7IcOBymnBeB0cNZQBQACBRkAAhwQaVafAAHkYVF9O5k2BA.png)

#### 面经广场

![微信图片_20250919160715_856_4.png](https://img.remit.ee/api/file/BQACAgUAAyEGAASHRsPbAAECLKVozQ8_swEc99aVtMWoJWWXKaozJQACCxkAAhwQaVaGGetIf2dxuTYE.png)

#### 填写简历

![微信图片_20250919160909_857_4.png](https://img.remit.ee/api/file/BQACAgUAAyEGAASHRsPbAAECLKdozQ-wEXJwKu6QmkvZ-qGkMkSuRgACDRkAAhwQaVY-g7mEgGcqyDYE.png)

#### 综合评估

![图片6.png](https://img.remit.ee/api/file/BQACAgUAAyEGAASHRsPbAAECLH9ozQ0CrFoFmq5qsT9Ule1PmeD1lQAC4hgAAhwQaVbY-A591mAdRzYE.png)

### 快速开始

1) 环境要求
- JDK 21、Maven 3.9+
- MySQL 8.x 与 Redis 6+
- 可选：阿里云 OSS、Iflytek API 账户

2) 初始化数据库
- 导入 `src/main/resources/virtual_interview_db_*.sql` 初始化表结构与基础数据。

3) 配置应用
- 在 `src/main/resources/application-prod.yml` 中设置数据库、Redis、OSS、Iflytek 等参数，或使用环境变量覆盖。

4) 构建与运行
```bash
# 构建
mvn clean package -DskipTests

# 运行（Jar）
java -jar target/virtual_interview-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

5) 访问与验证
- Swagger 文档：`http://localhost:8080/swagger-ui.html`
- 健康检查：`/actuator/health`

### 关键模块与能力映射
- **鉴权与基础设施**：`annotation/AuthCheck`、`aop/*`、`config/*`、`exception/*`、`common/*`
- **面试与聊天**：`controller/InterviewController`、`controller/ai/*`、`service/impl/*Interview*`
- **会话记忆**：`controller/ai/chatMemory/*`、`service/impl/*Memory*`（MySQL 持久化）
- **ASR/TTS**：`utils/rtasr/*`、`utils/tst/TtsService`、`model/dto/interview/audio/*`
- **表情/姿态**：`utils/face/IflytekExpressionService` 与 `flask-realtime-face-eye/`
- **RAG 知识库**：`controller/ai/rag/*`、`XfyunSparkKnowledge*`、`spring-ai-*` 相关配置
- **内容社区**：`Experience*` 与 `Channel*` 相关 `controller`、`service`、`mapper`、`model`
- **文件上传**：`OssUploadController` + `config/OssConfig`

### 开发与调试建议
- 使用 `application-dev.yml` 本地开发，并将敏感参数置于环境变量或 `.properties` 外部化。
- 开启 `mybatis-plus.configuration.log-impl` 可查看 SQL 日志。
- 利用 `spring-cache` + Redis 提升查询性能；热点数据可加过期策略与手动失效。
- 善用 `@Validated` 与全局错误码（`common/ErrorCode`）统一返回结构。

### 常见问题（FAQ）
- 启动失败：检查数据库/Redis/OSS/Iflytek 配置是否正确，网络是否可达。
- Swagger 无法访问：确认应用端口与 `springdoc` 配置；查看控制台是否有路径冲突。
- ASR/TTS 不工作：核对 Iflytek 凭据与区域、WebSocket/HTTP 接口地址是否匹配；查看 `asr_audio_logs/` 与 `tts_audio_logs/`。