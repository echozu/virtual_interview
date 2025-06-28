package com.echo.virtual_interview.iflytek.ws;

import jakarta.validation.constraints.NotNull;
import org.java_websocket.drafts.Draft;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ClientHandshakeBuilder;

public class DraftWithOrigin extends Draft_6455 {

    private final String originUrl;

    public DraftWithOrigin(String originUrl) {
        this.originUrl = originUrl;
    }

    @Override
    public Draft copyInstance() {
        return new DraftWithOrigin(originUrl);
    }

    @NotNull
    @Override
    public ClientHandshakeBuilder postProcessHandshakeRequestAsClient(@NotNull ClientHandshakeBuilder request) {
        super.postProcessHandshakeRequestAsClient(request);
        request.put("Origin", "https://" + originUrl);
        return request;
    }
}