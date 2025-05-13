package com.example.insightservice.integration;
import com.fasterxml.jackson.databind.JsonNode;

import com.example.insightservice.dto.GeminiPromptRequest;
import com.example.insightservice.dto.GeminiResponse;
import com.example.insightservice.dto.InsightRequest;
import com.example.insightservice.util.PromptBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class GeminiApiClient {

    @Value("${gemini.api-key}")
    private String apiKey;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash-001:generateContent?key=";


    public GeminiResponse requestToGemini(InsightRequest input) throws IOException {
        GeminiPromptRequest promptRequest = PromptBuilder.build(input);
        String json = objectMapper.writeValueAsString(promptRequest);

        OkHttpClient client = new OkHttpClient();
        RequestBody body = RequestBody.create(json, MediaType.get("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url(GEMINI_URL + apiKey)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Gemini API 호출 실패: " + response);
            }

            String result = response.body().string();

            // Gemini 응답에서 텍스트 추출
            JsonNode root = objectMapper.readTree(result);
            String rawText = root
                    .path("candidates").get(0)
                    .path("content")
                    .path("parts").get(0)
                    .path("text").asText();

            System.out.println("🎯 Gemini 응답 텍스트 = \n" + rawText);

            // JSON이 아니라 ```json 으로 감싸졌을 경우 제거
            String cleaned = rawText
                    .replaceAll("(?s)```json\\s*", "") // ```json 제거
                    .replaceAll("```", "")             // 마무리 ``` 제거
                    .trim();

            return objectMapper.readValue(cleaned, GeminiResponse.class);
        }
    }

}
