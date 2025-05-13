package com.example.insightservice.controller;

import com.example.insightservice.integration.GeminiVisionApiClient;
import com.example.insightservice.util.GeminiResponseParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/vision")
public class InsightVisionController {

    private final GeminiVisionApiClient geminiVisionApiClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/analyze")
    public ResponseEntity<?> analyzeImage(
            @RequestPart("image") MultipartFile image,
            @RequestPart("prompt") String prompt
    ) {
        try {
            // Gemini Vision 응답 (raw JSON string)
            String rawResponse = geminiVisionApiClient.analyzeImageWithPrompt(image, prompt);

            // 1. JSON 파싱해서 candidates[0].content.parts[0].text 추출
            JsonNode root = objectMapper.readTree(rawResponse);
            String text = root
                    .path("candidates").get(0)
                    .path("content")
                    .path("parts").get(0)
                    .path("text").asText();

            // 2. text를 우리가 원하는 구조로 파싱
            Map<String, Object> parsed = GeminiResponseParser.parse(text);

            // 3. 클라이언트에 JSON 응답
            return ResponseEntity.ok(parsed);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Gemini 분석 실패: " + e.getMessage());
        }
    }
}
