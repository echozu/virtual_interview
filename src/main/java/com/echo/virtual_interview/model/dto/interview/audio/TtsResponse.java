package com.echo.virtual_interview.model.dto.interview.audio;


import lombok.Data;

@Data
public class TtsResponse {
    private Header header;
    private Payload payload;

    @Data
    public static class Header {
        private int code;
        private String message;
        private String sid;
        private int status;
    }

    @Data
    public static class Payload {
        private Audio audio;
    }

    @Data
    public static class Audio {
        private String audio; // Base64编码的音频数据
        private int status;
        private int seq;
    }
}