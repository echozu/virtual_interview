# 面试视频分析API文档

本文档定义了前端与后端服务之间进行视频分析任务的API接口规范。整个流程涉及两个主要后端服务：

1. **Python分析服务**：负责接收视频文件，启动耗时的AI分析任务。
2. **Java业务服务**：负责接收分析结果、与大模型交互、持久化数据，并向前端提供轮询结果。

## 流程概览

1. **前端**向 **Python分析服务** 发送一个 `POST` 请求，上传视频文件和`sessionId`。
2. **Python分析服务** 立即返回一个唯一的 `analysisId`，并开始在后台异步处理视频。
3. **前端**拿到 `analysisId` 后，开始向 **Java业务服务** 的轮询接口发送 `GET` 请求。
4. **Python分析服务** 在处理完成后，会将详细结果发送给 **Java业务服务** 的一个内部接口。
5. **Java业务服务** 接收到结果，进行二次处理（如调用大模型、讯飞API），并将最终结果存入缓存。
6. **前端**通过轮询接口，最终从 **Java业务服务** 获取到 `COMPLETED` 状态和最终的分析结果。

## 1. Python分析服务接口

### 1.1 创建视频分析任务

此接口用于接收前端上传的视频文件，并创建一个异步分析任务。

- **Endpoint**: `/api/analyze_video`
- **Method**: `POST`
- **Content-Type**: `multipart/form-data`

#### 请求体 (Form Data)

| 参数名      | 类型   | 是否必须 | 描述                                    | 示例                 |
| ----------- | ------ | -------- | --------------------------------------- | -------------------- |
| `sessionId` | string | 是       | 当前面试的唯一会话ID。                  | `test-session-12345` |
| `video`     | File   | 是       | 用户上传的视频文件（如.mp4, .webm等）。 | (二进制文件数据)     |

#### 响应

##### 成功响应 (HTTP 200 OK)

```
{
  "status": "processing",
  "message": "分析任务已成功创建，正在后台处理中。",
  "analysisId": "test-session-12345-1751703076-a1b2c3"
}
```

| 字段名       | 类型   | 描述                                         |
| ------------ | ------ | -------------------------------------------- |
| `status`     | string | 固定为 `"processing"`，表示任务已接收。      |
| `message`    | string | 对状态的文字描述。                           |
| `analysisId` | string | 本次分析任务的唯一ID，前端需用此ID进行轮询。 |

##### 失败响应 (HTTP 400 Bad Request)

```
{
  "error": "请求中未找到视频文件"
}
```

## 2. Java业务服务接口

### 2.1 轮询分析结果

前端使用从Python服务获取的`analysisId`，定期调用此接口以获取任务的最新状态和最终结果。

- **Endpoint**: `/api/interview/analysis/result/{analysisId}`
- **Method**: `GET`
- **Content-Type**: `application/json`

#### 请求头 (Headers)

| Key             | Value                     | 描述                             |
| --------------- | ------------------------- | -------------------------------- |
| `Authorization` | `Bearer <your_jwt_token>` | 用户的身份验证令牌 (JWT Token)。 |

#### 路径参数 (Path Parameters)

| 参数名       | 类型   | 描述                       |
| ------------ | ------ | -------------------------- |
| `analysisId` | string | 要查询的分析任务的唯一ID。 |

#### 响应

响应体是一个包含`code`, `message`, `data`的通用结构。核心信息在`data`字段中。

##### 任务进行中 (PENDING)

```
{
  "code": 0,
  "message": "ok",
  "data": {
    "status": "PENDING",
    "data": "分析任务正在队列中，请稍候..."
  }
}
```

##### 任务已完成 (COMPLETED)

```
{
  "code": 0,
  "message": "ok",
  "data": {
    "status": "COMPLETED",
    "data": {
      "summary": "状态很棒！您看起来很自信，且全程保持了良好的眼神接触。",
      "suggestion": "",
      "status": "POSITIVE",
      "detailed_analysis": {
        "eye_contact_analysis": "平均注意力分数高，全程保持了高质量的眼神交流。",
        "emotional_state_analysis": "核心紧张度为'状态放松'，情绪积极稳定。"
      }
    }
  }
}
```

##### 任务失败 (FAILED)

```
{
  "code": 0,
  "message": "ok",
  "data": {
    "status": "FAILED",
    "data": "处理视频时发生错误：无法识别视频编码。"
  }
}
```

##### `data` 字段详解

| 字段名   | 类型   | 描述                                                         |
| -------- | ------ | ------------------------------------------------------------ |
| `status` | string | 任务的当前状态，可能的值为 `PENDING`, `COMPLETED`, `FAILED`。 |
| `data`   | any    | - 当`status`为`PENDING`或`FAILED`时，为一个描述性的字符串。<br>- 当`status`为`COMPLETED`时，为一个包含最终AI分析结果的JSON对象（即`RealtimeFeedbackDto`）。 |