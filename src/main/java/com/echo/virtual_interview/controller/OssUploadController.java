package com.echo.virtual_interview.controller;

import cn.hutool.core.lang.UUID;
import com.aliyun.oss.OSS;
import com.echo.virtual_interview.common.BaseResponse;
import com.echo.virtual_interview.common.ResultUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;

/**
 * oss文件上传
 */
@RestController
    @RequestMapping("/api/upload")
    public class OssUploadController {

        @Autowired
        private OSS ossClient;

        @Value("${echo.oss.bucket-name}")
        private String bucketName;

        @Value("${echo.oss.endpoint}")
        private String endpoint;

    /**
     * 上传频道的封面图片
     * @param file
     * @param request
     * @return
     */
        @PostMapping("/oss/channel")
        public BaseResponse<String> uploadToOss(@RequestParam("file") MultipartFile file,
                                                HttpServletRequest request) {
            try {
                // 1. 验证用户权限
                String userId = (String) request.getAttribute("user_id");
                if(userId == null) {
                    return ResultUtils.error(40100,"未登录");
                }

                // 2. 验证文件类型
                String contentType = file.getContentType();
                if (contentType != null && !contentType.startsWith("image/")) {
                    return ResultUtils.error(40000, "只能保存图片");
                }

                // 3. 生成唯一文件名
                String originalFilename = file.getOriginalFilename();
                String fileExt = originalFilename.substring(originalFilename.lastIndexOf("."));
                String fileName = "interview/channel/" + UUID.randomUUID() + fileExt;

                // 4. 上传到OSS
                ossClient.putObject(
                        bucketName,
                        fileName,
                        new ByteArrayInputStream(file.getBytes())
                );

                // 5. 构建访问URL
                String url = "https://" + bucketName + "." + endpoint + "/" + fileName;
                return ResultUtils.success(url);

            } catch (Exception e) {
                e.printStackTrace();
                return ResultUtils.error(50001, "上传失败");
            }
        }

    /**
     * 上传用户头像、简历图片
     * @param file
     * @param request
     * @return
     */
    @PostMapping("/oss/userAvatar")
    public BaseResponse<String> uploadToUserAvatarOss(@RequestParam("file") MultipartFile file,
                                        HttpServletRequest request) {
        try {
            // 1. 验证用户权限
            String userId = (String) request.getAttribute("user_id");
            if(userId == null) {
                return ResultUtils.error(40100,"未登录");
            }

            // 2. 验证文件类型
            String contentType = file.getContentType();
            if(!contentType.startsWith("image/")) {
                return ResultUtils.error(40000, "只能保存图片");
            }

            // 3. 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String fileExt = originalFilename.substring(originalFilename.lastIndexOf("."));
            String fileName = "interview/user/" + UUID.randomUUID() + fileExt;

            // 4. 上传到OSS
            ossClient.putObject(
                    bucketName,
                    fileName,
                    new ByteArrayInputStream(file.getBytes())
            );

            // 5. 构建访问URL
            String url = "https://" + bucketName + "." + endpoint + "/" + fileName;
            return ResultUtils.success(url);

        } catch (Exception e) {
            e.printStackTrace();
            return ResultUtils.error(50001, "上传失败");
        }
    }
    }