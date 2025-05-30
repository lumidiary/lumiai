package com.lumidiary.ai.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class CallbackSender {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public void send(String callbackUrl, Object payload) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            String json = objectMapper.writeValueAsString(payload);
            HttpEntity<String> entity = new HttpEntity<>(json, headers);

            restTemplate.postForEntity(callbackUrl, entity, String.class);
            log.info("[CallbackSender] 콜백 전송 완료: {}", callbackUrl);
        } catch (Exception e) {
            log.error("[CallbackSender] 콜백 전송 실패", e);
        }
    }
}
