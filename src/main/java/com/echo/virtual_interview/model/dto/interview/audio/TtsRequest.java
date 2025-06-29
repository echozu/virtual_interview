package com.echo.virtual_interview.model.dto.interview.audio;


import lombok.Builder;
import lombok.Data;
import java.util.Base64;
import java.nio.charset.StandardCharsets;

// 使用Lombok简化代码
@Data
@Builder
public class TtsRequest {
    private Header header;
    private Parameter parameter;
    private Payload payload;

    /**
     * 创建一个默认的、一次性发送的TTS请求
     * @param appId 应用ID
     * @param vcn 发音人
     * @param text 需要合成的文本
     * @return 构建好的请求对象
     */
    public static TtsRequest defaultSingleRequest(String appId, String vcn, String text) {
        String encodedText = Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));

        return TtsRequest.builder()
                .header(Header.builder().app_id(appId).build())
                .parameter(Parameter.builder()
                        .tts(Tts.builder()
                                .vcn(vcn)
                                .audio(Audio.builder().encoding("lame").sample_rate(24000).build())
                                .build())
                        .build())
                .payload(Payload.builder()
                        .text(Text.builder().text(encodedText).build())
                        .build())
                .build();
    }


    @Data
    @Builder
    public static class Header {
        private String app_id;
        @Builder.Default
        private int status = 2; // 默认设置为2，表示一次性发送所有文本
    }

    @Data
    @Builder
    public static class Parameter {
        private Tts tts;
        // private Oral oral; // 如果需要口语化设置，可以取消此行注释
    }

    @Data
    @Builder
    public static class Tts {
        private String vcn;
        @Builder.Default
        private int speed = 50;
        @Builder.Default
        private int volume = 50;
        @Builder.Default
        private int pitch = 50;
        private Audio audio;
    }

    @Data
    @Builder
    public static class Audio {
        private String encoding;
        private int sample_rate;
        @Builder.Default
        private int channels = 1;
        @Builder.Default
        private int bit_depth = 16;
        @Builder.Default
        private int frame_size = 0;
    }

    @Data
    @Builder
    public static class Payload {
        private Text text;
    }

    @Data
    @Builder
    public static class Text {
        @Builder.Default
        private String encoding = "utf8";
        @Builder.Default
        private String compress = "raw";
        @Builder.Default
        private String format = "plain";
        @Builder.Default
        private int status = 2; // 默认设置为2，表示一次性发送
        @Builder.Default
        private int seq = 0;
        private String text; // Base64编码后的文本
    }
}