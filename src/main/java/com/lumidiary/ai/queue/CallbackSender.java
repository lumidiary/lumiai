package com.lumidiary.ai.queue;

import com.lumidiary.ai.dto.ResponseDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class CallbackSender {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${callback.digest}")
    private String digestCallbackUrl;

    @Value("${callback.diary}")
    private String diaryCallbackUrl;

    public void send(String channel, ResponseDTO response) {
        String url = channel.equals("digest") ? digestCallbackUrl : diaryCallbackUrl;
        try {
            restTemplate.postForEntity(url, response, Void.class);
        } catch (Exception e) {
            System.err.println("Callback failed: " + e.getMessage());
            // TODO: 재시도 로직 추가 가능
        }
    }
}
