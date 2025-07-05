package com.echo.virtual_interview.utils.face;

import com.echo.virtual_interview.config.IflytekApiConfig;
import com.echo.virtual_interview.model.dto.interview.process.IflytekExpressionResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 核心服务类：用于调用讯飞人脸表情识别API。
 * 封装了请求头构建、签名计算和HTTP请求发送的全部逻辑。
 */
@Service
public class IflytekExpressionService {

    private static final String API_URL = "http://tupapi.xfyun.cn/v1/expression";

    @Autowired
    private IflytekApiConfig apiConfig; // 注入配置

    @Autowired
    private RestTemplate restTemplate; // 注入RestTemplate

    @Autowired
    private ObjectMapper objectMapper; // 注入Jackson ObjectMapper

    /**
     * 分析单张图片的表情
     *
     * @param imageBase64 图片的Base64编码字符串 (不含 "data:image/jpeg;base64,")
     * @param imageName   为该图片指定的名称，例如 "first_frame.jpg"
     * @return 解析后的API响应对象，使用Optional包装以处理可能的失败情况
     */
    public Optional<IflytekExpressionResponse> analyzeExpression(String imageBase64, String imageName) {
        if (imageBase64 == null || imageBase64.isEmpty()) {
            return Optional.empty();
        }

        try {
            // 1. 将Base64字符串解码为二进制数据
            byte[] imageBytes = Base64.decodeBase64(imageBase64);

            // 2. 构建请求头
            HttpHeaders headers = buildAuthHeaders(imageName);
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM); // 讯飞要求二进制流

            // 3. 创建HTTP实体
            HttpEntity<byte[]> requestEntity = new HttpEntity<>(imageBytes, headers);

            // 4. 发送POST请求
            ResponseEntity<String> response = restTemplate.postForEntity(API_URL, requestEntity, String.class);

            // 5. 检查响应并解析JSON
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                IflytekExpressionResponse expressionResponse = objectMapper.readValue(response.getBody(), IflytekExpressionResponse.class);
                return Optional.of(expressionResponse);
            } else {
                System.err.println("讯飞API请求失败: " + response.getStatusCode() + " | " + response.getBody());
                return Optional.empty();
            }

        } catch (Exception e) {
            System.err.println("调用讯飞API时发生异常: " + e.getMessage());
            e.printStackTrace();
            return Optional.empty();
        }
    }

    /**
     * 根据讯飞的鉴权规则构建HTTP请求头
     *
     * @param imageName 图片名称
     * @return 包含所有鉴权参数的HttpHeaders对象
     */
    private HttpHeaders buildAuthHeaders(String imageName) throws UnsupportedEncodingException, JsonProcessingException {
        String curTime = String.valueOf(System.currentTimeMillis() / 1000L);

        // 构建X-Param的原始JSON
        Map<String, String> paramMap = new HashMap<>();
        paramMap.put("image_name", imageName);
        String paramJson = objectMapper.writeValueAsString(paramMap);

        // 对X-Param进行Base64编码
        String paramBase64 = new String(Base64.encodeBase64(paramJson.getBytes("UTF-8")));

        // 计算X-CheckSum
        String checkSumPayload = apiConfig.getApikey() + curTime + paramBase64;
        String checkSum = DigestUtils.md5Hex(checkSumPayload);

        // 组装Headers
        HttpHeaders headers = new HttpHeaders();
        headers.put("X-Appid", Collections.singletonList(apiConfig.getAppid()));
        headers.put("X-CurTime", Collections.singletonList(curTime));
        headers.put("X-Param", Collections.singletonList(paramBase64));
        headers.put("X-CheckSum", Collections.singletonList(checkSum));

        return headers;
    }
}