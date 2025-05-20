package com.lumidiary.ai.integration;

import com.lumidiary.ai.dto.GeminiPromptRequest;
import com.lumidiary.ai.dto.GeminiResponse;
import com.lumidiary.ai.dto.VisionRequest;
import com.lumidiary.ai.dto.Metadata;
import com.lumidiary.ai.util.PromptBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class GeminiApiClient {

    @Value("${gemini.api-key}")
    private String apiKey;

    private final ObjectMapper objectMapper = new ObjectMapper();
    
    public GeminiResponse requestToGemini(VisionRequest input,
                                          Map<String, Metadata> metadataMap,
                                          Map<String, byte[]> imageBytesMap) throws IOException {
        GeminiPromptRequest promptRequest = PromptBuilder.buidVisionPrompt(input, metadataMap, imageBytesMap);
        String json = objectMapper.writeValueAsString(promptRequest);

        // 요청 본문 출력
        System.out.println("🎯 Gemini 요청 JSON = \n" + json);

        OkHttpClient client = new OkHttpClient();
        RequestBody body = RequestBody.create(json, MediaType.get("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url(GEMINI_URL + apiKey)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "";
                throw new IOException("Gemini API 호출 실패: " + response + " Error: " + errorBody);
            }
            String result = response.body().string();
            JsonNode root = objectMapper.readTree(result);
            String rawText = root
                    .path("candidates").get(0)
                    .path("content")
                    .path("parts").get(0)
                    .path("text").asText();

            System.out.println("🎯 Gemini 응답 텍스트 = \n" + rawText);
            String cleaned = rawText
                    .replaceAll("(?s)```json\\s*", "")
                    .replaceAll("```", "").trim();
            return objectMapper.readValue(cleaned, GeminiResponse.class);
        }
    }

    public String sendPromptRaw(GeminiPromptRequest request) throws IOException {
        String json = objectMapper.writeValueAsString(request);

        // 요청 본문 출력
        System.out.println("🎯 Gemini 요청 JSON = \n" + json);

        OkHttpClient client = new OkHttpClient();
        RequestBody body = RequestBody.create(json, MediaType.get("application/json; charset=utf-8"));
        Request httpRequest = new Request.Builder()
                .url(GEMINI_URL + apiKey)
                .post(body)
                .build();

        try (Response response = client.newCall(httpRequest).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "";
                throw new IOException("Gemini API 호출 실패: " + response + " Error: " + errorBody);
            }
            return response.body().string();
        }
    }

    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash-001:generateContent?key=";
}
