# -*- coding: utf-8 -*-
import base64
import random
import string
import threading

# 导入必要的库
from flask import Flask, request, jsonify
import cv2
import numpy as np
import dlib
from math import hypot
import os
import time
import uuid  # 用于生成唯一文件名
from flask_cors import CORS  # 导入CORS
from celery import Celery

# 导入我们新的分析模块
from analysis_module import analyze_interview_data
import requests
# --- 初始化 ---

# 1. 初始化 Flask 应用
app = Flask(__name__)
CORS(app)

# 2. 配置应用参数
app.config['MAIN_BACKEND_URL'] = 'http://123.207.53.16:9527/api/interview/process/python/video_analyse'
UPLOAD_FOLDER = 'uploads'
if not os.path.exists(UPLOAD_FOLDER):
    os.makedirs(UPLOAD_FOLDER)
app.config['UPLOAD_FOLDER'] = UPLOAD_FOLDER

# 3. 配置 Celery
app.config.update(
    broker_url='redis://:***REMOVED***@123.207.53.16:6379/9',
    result_backend='redis://:***REMOVED***@123.207.53.16:6379/9',
    result_expires=600  # 单位：秒（10分钟 = 600秒）
)

# 创建 Celery 实例
celery = Celery(app.name)
# 从 Flask 的 app.config 中加载配置
celery.conf.update(app.config)


# --- 4. 加载 dlib 模型和定义常量 ---
try:
    # ✅ [修正] 改为使用绝对路径加载模型文件，确保 Celery worker 能在任何位置找到它
    # 获取 app.py 文件所在的目录的绝对路径
    basedir = os.path.abspath(os.path.dirname(__file__))
    # 构建模型文件的绝对路径
    shape_predictor_path = os.path.join(basedir, "shape_predictor_68_face_landmarks.dat")

    if not os.path.exists(shape_predictor_path):
         print(f"错误：无法在以下路径找到模型文件: {shape_predictor_path}")
         print("请确保 'shape_predictor_68_face_landmarks.dat' 文件与 app.py 存在于同一个目录。")
         exit()

    detector = dlib.get_frontal_face_detector()
    predictor = dlib.shape_predictor(shape_predictor_path)

except RuntimeError as e:
    print(f"加载 dlib 模型时发生严重错误: {e}")
    print("这通常是由于模型文件损坏或与dlib版本不兼容导致的。")
    exit()


BLINK_RATIO_THRESHOLD = 4.5
FRAME_PROCESSING_INTERVAL = 3
FRAME_RESIZE_FACTOR = 0.5


# --- 辅助函数 ---

def midpoint(p1, p2):
    return int((p1.x + p2.x) / 2), int((p1.y + p2.y) / 2)


def get_blinking_ratio(eye_points, facial_landmark):
    left_point = (facial_landmark.part(eye_points[0]).x, facial_landmark.part(eye_points[0]).y)
    right_point = (facial_landmark.part(eye_points[3]).x, facial_landmark.part(eye_points[3]).y)
    center_top = midpoint(facial_landmark.part(eye_points[1]), facial_landmark.part(eye_points[2]))
    center_bottom = midpoint(facial_landmark.part(eye_points[5]), facial_landmark.part(eye_points[4]))
    hor_line_length = hypot((left_point[0] - right_point[0]), (left_point[1] - right_point[1]))
    vert_line_length = hypot((center_top[0] - center_bottom[0]), (center_top[1] - center_bottom[1]))
    return hor_line_length / vert_line_length if vert_line_length != 0 else 100.0


def get_gaze_ratio(frame, eye_points, facial_landmark, gray):
    left_eye_region = np.array([(facial_landmark.part(p).x, facial_landmark.part(p).y) for p in eye_points], np.int32)
    height, width, _ = frame.shape
    mask = np.zeros((height, width), np.uint8)
    cv2.polylines(mask, [left_eye_region], True, 255, 2)
    cv2.fillPoly(mask, [left_eye_region], 255)
    eye = cv2.bitwise_and(gray, gray, mask=mask)
    min_x, max_x = np.min(left_eye_region[:, 0]), np.max(left_eye_region[:, 0])
    min_y, max_y = np.min(left_eye_region[:, 1]), np.max(left_eye_region[:, 1])
    gray_eye = eye[min_y:max_y, min_x:max_x]
    if gray_eye.size == 0: return None
    _, threshold_eye = cv2.threshold(gray_eye, 70, 255, cv2.THRESH_BINARY)
    threshold_eye = cv2.bitwise_not(threshold_eye)
    height, width = threshold_eye.shape
    if width == 0: return None
    left_side_threshold = threshold_eye[0:height, 0:int(width / 2)]
    left_side_white = cv2.countNonZero(left_side_threshold)
    right_side_threshold = threshold_eye[0:height, int(width / 2):width]
    right_side_white = cv2.countNonZero(right_side_threshold)
    return left_side_white / right_side_white if right_side_white != 0 else (1 if left_side_white > 0 else None)


def get_head_pose(shape, frame_shape):
    model_points = np.array([(0.0, 0.0, 0.0), (0.0, -330.0, -65.0), (-225.0, 170.0, -135.0), (225.0, 170.0, -135.0),
                             (-150.0, -150.0, -125.0), (150.0, -150.0, -125.0)])
    image_points = np.array(
        [(shape.part(30).x, shape.part(30).y), (shape.part(8).x, shape.part(8).y), (shape.part(36).x, shape.part(36).y),
         (shape.part(45).x, shape.part(45).y), (shape.part(48).x, shape.part(48).y),
         (shape.part(54).x, shape.part(54).y)], dtype="double")
    focal_length, center = frame_shape[1], (frame_shape[1] / 2, frame_shape[0] / 2)
    camera_matrix = np.array([[focal_length, 0, center[0]], [0, focal_length, center[1]], [0, 0, 1]], dtype="double")
    (success, rotation_vector, translation_vector) = cv2.solvePnP(model_points, image_points, camera_matrix,
                                                                  np.zeros((4, 1)), flags=cv2.SOLVEPNP_ITERATIVE)
    rotation_matrix, _ = cv2.Rodrigues(rotation_vector)
    sy = np.sqrt(rotation_matrix[0, 0] * rotation_matrix[0, 0] + rotation_matrix[1, 0] * rotation_matrix[1, 0])
    singular = sy < 1e-6
    x = np.arctan2(rotation_matrix[2, 1], rotation_matrix[2, 2])
    y = np.arctan2(-rotation_matrix[2, 0], sy)
    z = np.arctan2(rotation_matrix[1, 0], rotation_matrix[0, 0]) if not singular else 0
    return np.degrees(x), np.degrees(y), np.degrees(z)


def get_facial_expression_proxies(landmarks):
    lip_top, lip_bottom = (landmarks.part(51).x, landmarks.part(51).y), (landmarks.part(57).x, landmarks.part(57).y)
    mouth_opening = hypot(lip_top[0] - lip_bottom[0], lip_top[1] - lip_bottom[1])
    mouth_left, mouth_right = (landmarks.part(48).x, landmarks.part(48).y), (landmarks.part(54).x, landmarks.part(54).y)
    mouth_width = hypot(mouth_left[0] - mouth_right[0], mouth_left[1] - mouth_right[1])
    face_width = hypot(landmarks.part(0).x - landmarks.part(16).x, landmarks.part(0).y - landmarks.part(16).y)
    if face_width == 0: return {'mouth_opening_ratio': 0, 'mouth_smile_ratio': 0}
    return {'mouth_opening_ratio': mouth_opening / face_width, 'mouth_smile_ratio': mouth_width / face_width}


# --- 主视频处理函数 ---
def process_video(video_path):
    if not os.path.exists(video_path):
        print(f"错误: 视频文件在路径 {video_path} 不存在！")
        return None

    cap = cv2.VideoCapture(video_path)

    if not cap.isOpened():
        print(f"错误: OpenCV (cv2.VideoCapture) 无法打开视频文件: {video_path}。")
        print("-> 可能的原因是：环境缺少处理.webm格式所需的视频解码器(如VP9)。请检查FFmpeg是否正确安装。")
        return None

    print(f"视频文件 {video_path} 已成功打开，开始逐帧处理。")

    fps = cap.get(cv2.CAP_PROP_FPS)
    if fps == 0:
        print("警告: 无法获取视频的FPS，将使用默认值 30。")
        fps = 30

    analysis_results_by_second = []
    frame_data_in_second = []
    current_second = 0
    total_frame_count = 0
    is_blinking = False

    first_frame_base64 = None
    last_frame_np = None

    while True:
        ret, frame = cap.read()
        if not ret:
            print("视频处理完成：已读取到所有帧。")
            break

        total_frame_count += 1

        if total_frame_count % FRAME_PROCESSING_INTERVAL != 0:
            continue

        if first_frame_base64 is None:
            success, buffer = cv2.imencode('.jpg', frame)
            if success:
                first_frame_base64 = base64.b64encode(buffer).decode('utf-8')

        last_frame_np = frame.copy()

        second_marker = int((total_frame_count - 1) / fps)

        if second_marker > current_second:
            if frame_data_in_second:
                attention_scores = [d['attention_score'] for d in frame_data_in_second]
                valid_frames = [d for d in frame_data_in_second if d['face_detected']]
                pitches = [d['head_pose']['pitch'] for d in valid_frames if 'pitch' in d['head_pose']]
                yaws = [d['head_pose']['yaw'] for d in valid_frames if 'yaw' in d['head_pose']]
                rolls = [d['head_pose']['roll'] for d in valid_frames if 'roll' in d['head_pose']]
                mouth_openings = [d['expression']['mouth_opening_ratio'] for d in valid_frames]
                mouth_smiles = [d['expression']['mouth_smile_ratio'] for d in valid_frames]

                analysis_results_by_second.append({
                    "time_in_seconds": current_second + 1,
                    "face_detection_rate": len(valid_frames) / len(frame_data_in_second) if frame_data_in_second else 0,
                    "attention_mean": np.mean(attention_scores) if attention_scores else 0,
                    "attention_std": np.std(attention_scores) if attention_scores else 0,
                    "blink_count": sum(d['is_blinking'] for d in frame_data_in_second),
                    "head_pose_mean": {"pitch": np.mean(pitches) if pitches else 0, "yaw": np.mean(yaws) if yaws else 0,
                                       "roll": np.mean(rolls) if rolls else 0},
                    "head_pose_std": {"pitch": np.std(pitches) if pitches else 0, "yaw": np.std(yaws) if yaws else 0,
                                      "roll": np.std(rolls) if rolls else 0},
                    "expression_mean": {"mouth_opening_ratio": np.mean(mouth_openings) if mouth_openings else 0,
                                        "mouth_smile_ratio": np.mean(mouth_smiles) if mouth_smiles else 0}
                })
            frame_data_in_second = []
            current_second = second_marker

        small_frame = cv2.resize(frame, (0, 0), fx=FRAME_RESIZE_FACTOR, fy=FRAME_RESIZE_FACTOR)
        gray = cv2.cvtColor(small_frame, cv2.COLOR_BGR2GRAY)

        faces = detector(gray)

        frame_analysis = {"face_detected": False, "attention_score": 0.0, "is_blinking": False, "head_pose": {},
                          "expression": {}}
        if len(faces) > 0:
            landmarks_small = predictor(gray, faces[0])
            points = [dlib.point(int(p.x / FRAME_RESIZE_FACTOR), int(p.y / FRAME_RESIZE_FACTOR)) for p in
                      landmarks_small.parts()]
            landmarks = dlib.full_object_detection(faces[0], points)

            frame_analysis.update({
                "face_detected": True,
                "attention_score": 0.3,
                "head_pose": dict(zip(['pitch', 'yaw', 'roll'], get_head_pose(landmarks, frame.shape))),
                "expression": get_facial_expression_proxies(landmarks)
            })

            blinking_ratio = (get_blinking_ratio([36, 37, 38, 39, 40, 41], landmarks) + get_blinking_ratio(
                [42, 43, 44, 45, 46, 47], landmarks)) / 2
            if blinking_ratio > BLINK_RATIO_THRESHOLD:
                if not is_blinking: frame_analysis["is_blinking"] = True
                is_blinking = True
            else:
                is_blinking = False

            original_gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
            gaze_ratio = get_gaze_ratio(frame, [36, 37, 38, 39, 40, 41], landmarks, original_gray)
            if gaze_ratio is not None and 0.8 < gaze_ratio < 2.2:
                frame_analysis["attention_score"] = 1.0
            else:
                frame_analysis["attention_score"] = 0.0

        frame_data_in_second.append(frame_analysis)

    last_frame_base64 = None
    if last_frame_np is not None:
        success, buffer = cv2.imencode('.jpg', last_frame_np)
        if success:
            last_frame_base64 = base64.b64encode(buffer).decode('utf-8')

    cap.release()

    return {
        "video_duration_seconds": round(total_frame_count / fps, 2) if fps > 0 else 0,
        "analysis_by_second": analysis_results_by_second,
        "first_frame_base64": first_frame_base64,
        "last_frame_base64": last_frame_base64
    }


# 新增：按指定时间间隔聚合数据的函数
def aggregate_data_by_interval(analysis_by_second, interval_seconds=30):
    if not analysis_by_second: return []
    aggregated_results = []
    num_seconds = len(analysis_by_second)
    for i in range(0, num_seconds, interval_seconds):
        chunk = analysis_by_second[i:i + interval_seconds]
        if not chunk: continue

        start_time, end_time = chunk[0]['time_in_seconds'], chunk[-1]['time_in_seconds']
        attention_means = [s['attention_mean'] for s in chunk]
        pitch_means, yaw_means, roll_means = ([s['head_pose_mean']['pitch'] for s in chunk],
                                              [s['head_pose_mean']['yaw'] for s in chunk],
                                              [s['head_pose_mean']['roll'] for s in chunk])

        aggregated_results.append({
            "start_time_seconds": start_time,
            "end_time_seconds": end_time,
            "interval_duration_seconds": len(chunk),
            "face_detection_rate": round(np.mean([s['face_detection_rate'] for s in chunk]), 3),
            "attention_mean": round(np.mean(attention_means), 3),
            "attention_stability": round(np.std(attention_means), 3),
            "total_blinks": sum(s['blink_count'] for s in chunk),
            "head_pose": {
                "pitch_mean": round(np.mean(pitch_means), 2), "yaw_mean": round(np.mean(yaw_means), 2),
                "roll_mean": round(np.mean(roll_means), 2),
                "pitch_fluctuation": round(np.std(pitch_means), 2), "yaw_fluctuation": round(np.std(yaw_means), 2),
                "roll_fluctuation": round(np.std(roll_means), 2)
            },
            "expression": {
                "mouth_opening_mean": round(np.mean([s['expression_mean']['mouth_opening_ratio'] for s in chunk]), 4),
                "mouth_smile_mean": round(np.mean([s['expression_mean']['mouth_smile_ratio'] for s in chunk]), 4)
            }
        })
    return aggregated_results


def calculate_nervousness_score(half_minute_data):
    """
    根据半分钟的数据块，生成一份详细的、结构化的紧张度分析报告。
    这份报告为二次AI分析提供了量化的特征、阈值和判断依据。
    :param half_minute_data: analysis_by_half_minute 列表中的一个元素
    :return: 一个包含详细分析的字典
    """
    THRESHOLDS = {
        'blinks': {'normal_range': [8, 15], 'high_threshold': 20, 'unit': '次/半分钟'},
        'attention_stability': {'high_threshold': 0.3, 'unit': '标准差'},
        'head_yaw_fluctuation': {'high_threshold': 4.0, 'unit': '度/标准差'},
        'face_detection_rate': {'low_threshold': 0.9, 'unit': '比例'}
    }
    total_score = 0
    components = {}
    blinks_val = half_minute_data['total_blinks']
    blinks_config = THRESHOLDS['blinks']
    blinks_score = 0
    blinks_comment = "眨眼频率在正常范围内。"
    if blinks_val > blinks_config['high_threshold']:
        blinks_score = 3
        blinks_comment = "眨眼频率显著高于正常范围，是压力的强指标。"
    elif blinks_val > blinks_config['normal_range'][1]:
        blinks_score = 1
        blinks_comment = "眨眼频率偏高，可能存在轻微紧张。"
    components['blinks'] = {
        "value": blinks_val, "unit": blinks_config['unit'],
        "normal_range": blinks_config['normal_range'],
        "score_contribution": blinks_score, "comment": blinks_comment
    }
    total_score += blinks_score
    attention_val = round(half_minute_data['attention_stability'], 3)
    attention_config = THRESHOLDS['attention_stability']
    attention_score = 0
    attention_comment = "注意力稳定，视线接触良好。"
    if attention_val > attention_config['high_threshold']:
        attention_score = 2
        attention_comment = "注意力稳定性低于阈值，表明视线可能存在回避或飘忽。"
    components['attention_stability'] = {
        "value": attention_val, "unit": attention_config['unit'],
        "threshold_high": attention_config['high_threshold'],
        "score_contribution": attention_score, "comment": attention_comment
    }
    total_score += attention_score
    yaw_fluctuation_val = round(half_minute_data['head_pose']['yaw_fluctuation'], 2)
    yaw_config = THRESHOLDS['head_yaw_fluctuation']
    yaw_score = 0
    yaw_comment = "头部姿态稳定，未见明显不安的小动作。"
    if yaw_fluctuation_val > yaw_config['high_threshold']:
        yaw_score = 2
        yaw_comment = "头部左右晃动幅度偏高，可能是不安或小动作增多的体现。"
    components['head_yaw_fluctuation'] = {
        "value": yaw_fluctuation_val, "unit": yaw_config['unit'],
        "threshold_high": yaw_config['high_threshold'],
        "score_contribution": yaw_score, "comment": yaw_comment
    }
    total_score += yaw_score
    face_rate_val = round(half_minute_data['face_detection_rate'], 2)
    face_rate_config = THRESHOLDS['face_detection_rate']
    face_rate_score = 0
    face_rate_comment = "面部检出率正常，未发现明显的用手遮挡行为。"
    if face_rate_val < face_rate_config['low_threshold']:
        face_rate_score = 3
        face_rate_comment = "面部检出率较低，可能存在频繁用手遮挡脸部的行为，这是紧张的典型信号。"
    components['face_detection_rate'] = {
        "value": face_rate_val, "unit": face_rate_config['unit'],
        "threshold_low": face_rate_config['low_threshold'],
        "score_contribution": face_rate_score, "comment": face_rate_comment
    }
    total_score += face_rate_score
    if total_score >= 5:
        level = "高度紧张"
    elif total_score >= 3:
        level = "中度紧张"
    elif total_score >= 1:
        level = "轻微紧张"
    else:
        level = "状态放松"
    return {
        "overall_score": total_score,
        "level": level,
        "components": components
    }


# --- Celery 任务定义 ---
@celery.task
def run_analysis_and_forward(video_path, session_id, analysis_id, main_backend_url):
    """
    这个函数现在是一个 Celery 任务，将由 Celery worker 在后台执行。
    """
    final_response_to_main_backend = None
    try:
        print(f"Celery 任务 [{analysis_id}] 开始分析视频: {video_path}")
        start_time = time.time()

        raw_results = process_video(video_path)

        if raw_results is None or not raw_results.get("analysis_by_second"):
            print(f"错误 [{analysis_id}]: 视频处理失败或未在视频中检测到有效活动。任务终止。")
            error_response = {
                "analysisId": analysis_id,
                "sessionId": session_id,
                "status": "error",
                "message": "视频处理失败：无法打开视频文件或视频内容无法分析。"
            }
            try:
                requests.post(main_backend_url, json=error_response, timeout=20).raise_for_status()
                print(f"Celery 任务 [{analysis_id}]: 已成功将错误状态转发至主后端。")
            except requests.exceptions.RequestException as e:
                print(f"错误 [{analysis_id}]: 转发错误状态至主后端失败: {e}")
            return

        analysis_data = raw_results["analysis_by_second"]
        half_minute_report = aggregate_data_by_interval(analysis_data, 30)
        for report_block in half_minute_report:
            nervousness_analysis = calculate_nervousness_score(report_block)
            report_block['nervousness_analysis'] = nervousness_analysis

        interview_summary = analyze_interview_data(half_minute_report)
        end_time = time.time()
        print(f"Celery 任务 [{analysis_id}] 分析完成，耗时: {end_time - start_time:.2f} 秒")

        final_response_to_main_backend = {
            "analysisId": analysis_id,
            "sessionId": session_id,
            "status": "success",
            "message": "视频分析成功",
            "analysis_by_half_minute": half_minute_report,
            "overall_summary": interview_summary,
            "first_frame_base64": raw_results.get("first_frame_base64"),
            "last_frame_base64": raw_results.get("last_frame_base64")
        }

        print(f"Celery 任务 [{analysis_id}] 正在将精简后的结果转发至: {main_backend_url}")
        try:
            requests.post(main_backend_url, json=final_response_to_main_backend, timeout=20).raise_for_status()
            print(f"Celery 任务 [{analysis_id}] 成功将数据转发至主后端。")
        except requests.exceptions.RequestException as e:
            print(f"错误 [{analysis_id}]: 转发至主后端失败: {e}")

    except Exception as e:
        print(f"Celery 任务 [{analysis_id}] 处理过程中发生严重错误: {e}")
        # 这里也可以向主后端发送一个错误报告

    finally:
        if os.path.exists(video_path):
            try:
                os.remove(video_path)
                print(f"Celery 任务 [{analysis_id}]: 已删除临时视频文件 {video_path}")
            except OSError as e:
                print(f"错误 [{analysis_id}]: 删除临时文件 {video_path} 失败: {e}")

        if final_response_to_main_backend:
            printable_response = final_response_to_main_backend.copy()
            printable_response.pop("first_frame_base64", None)
            printable_response.pop("last_frame_base64", None)
            print(f"Celery 任务 [{analysis_id}]: 发送后端的值为： {printable_response}")

# --- Flask API 路由 ---
@app.route('/api/analyze_video', methods=['POST'])
def analyze_video_api():
    if 'video' not in request.files: return jsonify({"error": "请求中未找到视频文件"}), 400
    if 'sessionId' not in request.form: return jsonify({"error": "请求中缺少 sessionId"}), 400

    file = request.files['video']
    session_id = request.form.get('sessionId')
    if file.filename == '': return jsonify({"error": "未选择文件"}), 400

    timestamp = int(time.time())
    random_str = ''.join(random.choices(string.ascii_lowercase + string.digits, k=6))
    analysis_id = f"{session_id}-{timestamp}-{random_str}"
    # 确保文件名是安全的，虽然我们用了uuid，但这是个好习惯
    from werkzeug.utils import secure_filename
    safe_filename = secure_filename(f"{analysis_id}.webm")
    video_path = os.path.join(app.config['UPLOAD_FOLDER'], safe_filename)
    file.save(video_path)

    main_backend_url = app.config['MAIN_BACKEND_URL']
    run_analysis_and_forward.delay(video_path, session_id, analysis_id, main_backend_url)

    print(f"已为会话 {session_id} 创建 Celery 任务，ID为: {analysis_id}")

    return jsonify({
        "status": "processing",
        "message": "分析任务已成功创建，正在后台处理中。",
        "analysisId": analysis_id
    })

# --- 主程序入口 ---
if __name__ == '__main__':
    ip_address = "0.0.0.0"
    port = 55274
    print(f"✅ API服务启动成功，正在监听 http://{ip_address}:{port}")
    print(f"🚀 请通过 POST 请求访问 /api/analyze_video 接口以上传视频进行分析")
    # 在开发环境中，Flask 的重载器 (reloader) 可能会导致 dlib 模型被加载两次。
    # 设置 use_reloader=False 可以避免这个问题，但在生产环境中通常用 Gunicorn 等服务器，不受此影响。
    app.run(host=ip_address, port=port, debug=True, use_reloader=False)