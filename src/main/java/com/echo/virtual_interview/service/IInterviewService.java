package com.echo.virtual_interview.service;

import reactor.core.publisher.Flux;

public interface IInterviewService {
    Flux<String> interviewProcess(String message, String chatId);
}
