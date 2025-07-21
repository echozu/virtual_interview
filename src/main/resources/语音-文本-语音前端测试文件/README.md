# WebSocket 音频流测试工具（讯飞ASR版）- AudioWorklet最终版

## 项目说明

这是一个使用 AudioWorklet 技术的高性能音频流测试工具，专门用于与讯飞语音识别引擎进行实时音频流通信。

## 核心特性

- ✅ **AudioWorklet 技术**：音频处理在独立后台线程，避免主线程阻塞
- ✅ **高质量音频处理**：线性插值降采样，16kHz PCM 格式
- ✅ **实时音频流**：稳定的时序控制，符合讯飞引擎要求
- ✅ **WebSocket 通信**：支持 STOMP 协议的实时双向通信
- ✅ **录音下载**：支持将录音保存为 WAV 文件

## 文件结构

```
├── websocket-audio-test.html    # 主测试页面
├── recorder.worklet.js          # AudioWorklet 处理器
├── test-worklet.html           # AudioWorklet 测试页面
├── server.js                   # Node.js HTTP 服务器
└── README.md                   # 说明文档
```

## 快速开始

### 方法一：使用 Node.js 服务器（推荐）

1. **启动服务器**
   ```bash
   node server.js
   ```

2. **访问测试页面**
   - 主测试页面：http://localhost:3000/websocket-audio-test.html
   - AudioWorklet 测试页面：http://localhost:3000/test-worklet.html

### 方法二：使用 Python 服务器

1. **启动服务器**
   ```bash
   python -m http.server 8080
   ```

2. **访问测试页面**
   - 主测试页面：http://localhost:8080/websocket-audio-test.html
   - AudioWorklet 测试页面：http://localhost:8080/test-worklet.html

## 重要说明

### ⚠️ 必须通过 HTTP 服务器访问

AudioWorklet 模块无法通过 `file://` 协议直接加载，必须通过 HTTP/HTTPS 服务器访问页面。

### 🔧 测试步骤

1. **首先测试 AudioWorklet**
   - 访问 `test-worklet.html`
   - 点击"测试 AudioWorklet 加载"按钮
   - 确认加载成功后，测试录音功能

2. **使用主测试页面**
   - 访问 `websocket-audio-test.html`
   - 配置 WebSocket 连接参数
   - 连接成功后开始录音测试

## 技术架构

### AudioWorklet 优势

- **独立线程**：音频处理在后台线程，不受主线程卡顿影响
- **实时性**：保证音频数据的时序准确性
- **高性能**：零拷贝数据传输，线性插值降采样
- **现代标准**：浏览器官方推荐的音频处理方案

### 音频处理流程

```
麦克风输入 → AudioWorklet 处理 → 降采样(16kHz) → PCM转换 → 发送到服务器
```

## 故障排除

### 常见问题

1. **"Unable to load a worklet's module"**
   - 确保通过 HTTP 服务器访问页面
   - 检查 `recorder.worklet.js` 文件是否存在
   - 查看浏览器控制台的详细错误信息

2. **录音没有声音**
   - 检查麦克风权限
   - 确认浏览器支持 AudioWorklet
   - 查看控制台是否有错误信息

3. **WebSocket 连接失败**
   - 检查服务器地址和端口
   - 确认 JWT Token 有效
   - 检查网络连接

### 浏览器兼容性

- Chrome 66+
- Firefox 76+
- Safari 14.1+
- Edge 79+

## 开发说明

### 修改 AudioWorklet 处理器

编辑 `recorder.worklet.js` 文件：

```javascript
class RecorderProcessor extends AudioWorkletProcessor {
    // 修改音频处理逻辑
    process(inputs) {
        // 自定义处理代码
    }
}
```

### 自定义音频参数

在 HTML 文件中修改：

```javascript
const chunkSize = 1280;    // 音频块大小
const sendInterval = 40;    // 发送间隔(毫秒)
```

## 许可证

MIT License 


注意事项
1.语音转写的接口，需要与recorder.worklet.js一起
即：websocket-audio+recorder.worklet.js