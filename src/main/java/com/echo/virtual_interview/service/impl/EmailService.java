package com.echo.virtual_interview.service.impl;


import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * 邮件服务实现类
 *
 * @author Heritage Team
 * @description 处理邮件发送和验证码生成
 */
@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    // 从配置文件中读取发送人邮箱
    @Value("${spring.mail.username}")
    private String fromEmail;

    /**
     * 生成并发送验证码
     */
    public String generateAndSendCaptcha(String email) throws MessagingException {
        // 生成6位数字验证码
        String captcha = generateCaptcha();

        // 存储到Redis，有效期5分钟
        String key = "captcha:" + email;
        redisTemplate.opsForValue().set(key, captcha, 5, TimeUnit.MINUTES);

        // 发送邮件
        sendCaptchaEmail(email, captcha);

        return captcha;
    }

    /**
     * 生成6位数字验证码（优化版本）
     */
    private String generateCaptcha() {
        return String.valueOf(new Random().nextInt(900000) + 100000);
    }

    /**
     * 发送验证码邮件（AI模拟面试平台专用）
     */
    private void sendCaptchaEmail(String email, String captcha) throws MessagingException {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);

        helper.setFrom(fromEmail);
        helper.setTo(email);
        helper.setSubject("🤖 AI模拟面试平台 - 邮箱验证码");

        String htmlContent = buildHtmlEmailContent(captcha);
        helper.setText(htmlContent, true);

        mailSender.send(mimeMessage);
        System.out.println("验证码已发送至邮箱：" + email);
    }

    /**
     * 构建AI模拟面试平台风格的HTML邮件
     */
    private String buildHtmlEmailContent(String captcha) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta charset='UTF-8'>" +
                "<style>" +
                "  .email-container { font-family: 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; max-width: 600px; margin: 0 auto; }" +
                "  .header { background: linear-gradient(135deg, #6e8efb, #a777e3); color: white; padding: 25px; text-align: center; border-radius: 8px 8px 0 0; }" +
                "  .content { padding: 25px; background: #ffffff; border: 1px solid #e0e0e0; border-top: none; }" +
                "  .code-box { background: #f5f7ff; border: 2px solid #e0e6ff; border-radius: 6px; padding: 15px; text-align: center; margin: 20px 0; font-size: 28px; font-weight: 600; color: #4a6cf7; }" +
                "  .footer { margin-top: 30px; font-size: 12px; color: #999; text-align: center; }" +
                "  .ai-icon { width: 60px; height: 60px; margin-bottom: 15px; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class='email-container'>" +
                "  <div class='header'>" +
                "    <h1 style='margin:0;'>AI模拟面试平台</h1>" +
                "    <p style='margin:5px 0 0; opacity:0.9;'>您的智能面试助手</p>" +
                "  </div>" +
                "  <div class='content'>" +
                "    <p>尊敬的求职者，您好！</p>" +
                "    <p>您正在注册AI模拟面试平台，这是您的邮箱验证码：</p>" +
                "    <div class='code-box'>" + captcha + "</div>" +
                "    <p>验证码将在 <strong style='color:#4a6cf7;'>5分钟</strong> 后失效，请及时使用。</p>" +
                "    <p>如果不是您本人操作，请忽略此邮件。</p>" +
                "    <div class='footer'>" +
                "      <p>💡 提示：使用Chrome或Edge浏览器可获得最佳面试体验</p>" +
                "      <p>© 2025 AI模拟面试平台 - 用科技赋能求职之路</p>" +
                "    </div>" +
                "  </div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }

    /**
     * 验证验证码
     */
    public boolean verifyCaptcha(String email, String captcha) {
        String key = "captcha:" + email;
        String storedCaptcha = redisTemplate.opsForValue().get(key);
        return storedCaptcha != null && storedCaptcha.equals(captcha);
    }
}
