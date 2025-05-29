package com.lumidiary.ai.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumidiary.ai.dto.*;
import com.lumidiary.ai.util.PromptBuilder;
import lombok.RequiredArgsConstructor;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

@Component
@RequiredArgsConstructor
public class GeminiApiClient {

    @Value("${gemini.api-key}")
    private String apiKey;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash-001:generateContent?key=";

    /**
     * Vision 전용: 이미지+텍스트 입력으로 GeminiResponse 구조 응답 반환
     */
    public GeminiResponse requestToGemini(VisionRequest input,
                                          Map<String, Metadata> metadataMap,
                                          Map<String, byte[]> imageBytesMap) throws IOException {

        GeminiPromptRequest promptRequest = PromptBuilder.buidVisionPrompt(input, metadataMap, imageBytesMap);
        String json = objectMapper.writeValueAsString(promptRequest);

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

            String cleaned = rawText
                    .replaceAll("(?s)```json\\s*", "")
                    .replaceAll("```", "")
                    .trim();

            JsonNode node = objectMapper.readTree(cleaned);
            GeminiResponse responseDto = new GeminiResponse();

            List<String> questionList = new ArrayList<>();
            JsonNode questionsNode = node.get("questions");
            if (questionsNode != null && questionsNode.isArray()) {
                for (JsonNode q : questionsNode) {
                    questionList.add(q.asText());
                }
            }
            responseDto.setQuestions(questionList);

            responseDto.setOverallDaySummary(node.path("overallDaySummary").asText(""));
            responseDto.setLanguage(input.getUserLocale());

            List<GeminiResponse.ImageDescription> descriptions = new ArrayList<>();
            for (VisionRequest.ImageData image : input.getImages()) {
                GeminiResponse.ImageDescription desc = new GeminiResponse.ImageDescription();
                desc.setImageId(image.getId());
                desc.setDescription("설명이 제공되지 않았습니다");
                desc.setMetadata(metadataMap.get(image.getId()));
                descriptions.add(desc);
            }
            responseDto.setImageDescriptions(descriptions);
            responseDto.setImages(descriptions);

            return responseDto;
        }
    }

    /**
     * Digest 전용: 프롬프트 전송 후 원시 응답을 String으로 반환
     */
    public String sendPromptRaw(GeminiPromptRequest request) throws IOException {
        String json = objectMapper.writeValueAsString(request);

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
}
