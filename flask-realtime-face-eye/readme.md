# 实时人脸识别与眼球追踪系统

# 1.flask

1.pip install -r requirements.txt

2.如果dlib 无法下载

windows:搜索相关

linux：sudo yum install -y cmake gcc gcc-c++

再下载即可

3.启动：python app.py

端口就是app.py 定义的端口 然后再哪里启动 ip就是哪，比如在本地，那就是localhost:55274

```python
# --- 主程序入口 ---
if __name__ == '__main__':
    ip_address = "0.0.0.0"
    port = 55274
    print(f"✅ API服务启动成功，正在监听 http://{ip_address}:{port}")
    print(f"🚀 请通过 POST 请求访问 /api/analyze_video 接口以上传视频进行分析")
    app.run(host=ip_address, port=port, debug=True)
```

其中有访问后端的服务：在app.py 的顶部定义了

```python
app = Flask(__name__)
# 启用CORS，允许你的前端项目进行跨域调用
CORS(app)
app.config['MAIN_BACKEND_URL'] = 'http://123.207.53.16:9527/api/interview/process/python/video_analyse'
```

4.测试文件：

在FLASK-Real-time-face-recognition-and-eye-ball-tracking-master/templates/upload.html

其中：访问python的（上传视频） 不需要token

但后续的轮询需要token【在代码里面】

# 2.原生

## 功能简介

本项目是一个基于Flask的实时人脸识别与眼球追踪系统，具有以下主要功能：

- **实时人脸检测**：使用OpenCV的Haar级联分类器进行人脸检测
- **眼球追踪**：基于dlib的68点面部特征点检测，实现眼球运动追踪
- **注意力分析**：通过分析眼球位置判断用户是否专注于屏幕
- **Web界面**：提供Flask Web界面，支持实时视频流显示
- **数据记录**：自动记录注意力数据并生成可视化图表
- **双模式运行**：支持本地运行和Web流式传输两种模式

## 项目结构

```
FLASK-Real-time-face-recognition-and-eye-ball-tracking-master/
├── main.py                          # 本地运行模式主程序
├── webstreaming.py                  # Flask Web流式传输模式
├── templates/
│   └── index.html                   # Web界面模板
├── haarcascade_frontalface_default.xml  # OpenCV人脸检测模型
├── shape_predictor_68_face_landmarks.dat # dlib面部特征点模型
├── landmark.PNG                     # 面部特征点示意图
├── log.txt                          # 注意力数据记录文件
└── pyimagesearch/
    └── __init__.py                  # 包初始化文件
```

## 各文件功能详解

### 核心程序文件

#### `main.py` - 本地运行模式
- **功能**：本地摄像头实时人脸识别与眼球追踪
- **特点**：
  - 直接调用本地摄像头
  - 实时显示检测结果
  - 程序结束后自动生成注意力曲线图
  - 适合单机使用和调试

#### `webstreaming.py` - Web流式传输模式
- **功能**：基于Flask的Web实时视频流
- **特点**：
  - 支持多用户同时访问
  - 线程安全的视频流处理
  - 可通过浏览器远程访问
  - 适合网络部署和远程监控

### 模型文件

#### `haarcascade_frontalface_default.xml`
- **用途**：OpenCV Haar级联分类器模型
- **功能**：进行人脸检测和定位
- **来源**：OpenCV官方预训练模型

#### `shape_predictor_68_face_landmarks.dat`
- **用途**：dlib面部特征点检测模型
- **功能**：检测面部68个关键点，用于眼球追踪
- **大小**：约68MB，需要单独下载

### 界面文件

#### `templates/index.html`
- **功能**：Flask Web界面模板
- **内容**：简单的HTML页面，显示实时视频流

### 数据文件

#### `log.txt`
- **功能**：记录每秒的注意力百分比数据
- **格式**：每行一个浮点数，表示该秒的注意力百分比

## Anaconda环境配置

### 1. 创建新的conda环境

```bash
conda create -n face_tracking python=3.8
conda activate face_tracking
```

### 2. 安装核心依赖

```bash
# 安装OpenCV
conda install -c conda-forge opencv

# 安装dlib（可能需要一些时间）
conda install -c conda-forge dlib

# 安装其他依赖
conda install numpy matplotlib
pip install flask imutils
```

### 3. 验证安装

```bash
python -c "import cv2; import dlib; import flask; print('所有依赖安装成功！')"
```

### 4. 下载模型文件

如果`shape_predictor_68_face_landmarks.dat`文件不存在，需要下载：

```bash
# 方法1：使用wget（Linux/Mac）
wget http://dlib.net/files/shape_predictor_68_face_landmarks.dat.bz2
bunzip2 shape_predictor_68_face_landmarks.dat.bz2

# 方法2：手动下载
# 访问 http://dlib.net/files/shape_predictor_68_face_landmarks.dat.bz2
# 下载后解压到项目根目录
```

## Flask配置

### 基本配置

项目使用Flask框架提供Web服务，主要配置包括：

- **主机地址**：通过命令行参数指定（默认0.0.0.0）
- **端口号**：通过命令行参数指定（建议8000-65535）
- **调试模式**：开发时启用debug=True
- **多线程**：启用threaded=True支持并发访问

### 路由配置

- `/`：主页面，显示视频流界面
- `/video_feed`：视频流端点，提供MJPEG格式的视频流

## 使用方法

### 方法一：本地运行模式

1. **激活环境**
   ```bash
   conda activate face_tracking
   ```

2. **运行程序**
   ```bash
   python main.py
   ```

3. **操作说明**
   - 程序启动后会打开摄像头窗口
   - 绿色框表示检测到的人脸
   - "ON-SCREEN"表示注意力在屏幕上
   - "OUTSIDE"表示注意力偏离屏幕
   - "NO FACE DETECTED"表示未检测到人脸
   - 按ESC键退出程序

4. **查看结果**
   - 程序结束后会自动显示注意力曲线图
   - 数据保存在`log.txt`文件中

### 方法二：Web流式传输模式

1. **激活环境**
   ```bash
   conda activate face_tracking
   ```

2. **运行Flask服务**
   ```bash
   python webstreaming.py --ip 0.0.0.0 --port 8000
   ```

3. **访问Web界面**
   - 打开浏览器访问 `http://localhost:8000`
   - 或从其他设备访问 `http://[服务器IP]:8000`

4. **参数说明**
   - `--ip`：指定服务器IP地址
   - `--port`：指定端口号
   - `--frame-count`：背景模型帧数（可选）

### 注意事项

1. **摄像头权限**：确保系统允许程序访问摄像头
2. **模型文件**：确保`shape_predictor_68_face_landmarks.dat`文件存在
3. **网络访问**：Web模式需要配置防火墙允许相应端口
4. **性能要求**：建议使用性能较好的设备以获得流畅体验

## 技术原理

### 人脸检测
- 使用OpenCV的Haar级联分类器
- 基于Haar特征的机器学习方法
- 实时检测视频流中的人脸

### 眼球追踪
- 使用dlib的68点面部特征点检测
- 通过计算眼球区域的宽高比判断注视方向
- 分析眼球在眼眶中的位置分布

### 注意力分析
- 基于眼球位置计算注意力百分比
- 每秒统计一次注意力数据
- 生成实时注意力曲线图

## 故障排除

### 常见问题

1. **dlib安装失败**
   ```bash
   # 尝试使用conda安装
   conda install -c conda-forge dlib
   
   # 或使用pip安装预编译版本
   pip install dlib-binary
   ```

2. **摄像头无法访问**
   - 检查摄像头是否被其他程序占用
   - 确认系统摄像头权限设置
   - 尝试重启程序

3. **模型文件缺失**
   - 下载`shape_predictor_68_face_landmarks.dat`文件
   - 确保文件放在项目根目录

4. **Flask端口被占用**
   ```bash
   # 使用不同端口
   python webstreaming.py --ip 0.0.0.0 --port 8001
   python webstreaming.py -i 127.0.0.1 -o 5000
   ```

