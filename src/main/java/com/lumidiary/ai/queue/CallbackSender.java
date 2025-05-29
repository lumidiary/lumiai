package com.lumidiary.ai.queue;

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

    /**
     * digest → digestCallbackUrl
     * diary 또는 vision → diaryCallbackUrl 사용
     */
    public void send(String channel, Object response) {
        String url = switch (channel) {
            case "digest" -> digestCallbackUrl;
            case "vision", "diary" -> diaryCallbackUrl; // 🔁 vision은 diary로 처리
            default -> diaryCallbackUrl; // 기본 fallback
        };

        try {
            restTemplate.postForEntity(url, response, Void.class);
        } catch (Exception e) {
            System.err.println(" Callback 전송 실패 (" + channel + "): " + e.getMessage());
        }
    }
}
