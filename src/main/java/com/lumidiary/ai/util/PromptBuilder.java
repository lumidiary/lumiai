package com.lumidiary.ai.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumidiary.ai.dto.GeminiPromptRequest;
import com.lumidiary.ai.dto.InsightRequest;
import com.lumidiary.ai.dto.Metadata;
import com.lumidiary.ai.dto.DigestRequestForPrompt;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PromptBuilder {

    // promptType -> promptText 매핑
    private static final Map<PromptType, String> SYSTEM_PROMPTS = loadSystemPrompts();

    private static Map<PromptType, String> loadSystemPrompts() {
        Map<PromptType, String> prompts = new HashMap<>();
        for (PromptType type : PromptType.values()) {
            prompts.put(type, loadPrompt(type.getPath()));
        }
        return prompts;
    }

    private static String loadPrompt(String resourcePath) {
        try (InputStream in = PromptBuilder.class.getResourceAsStream(resourcePath);
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            if (in == null) {
                throw new RuntimeException(resourcePath + " not found");
            }
            return reader.lines().collect(Collectors.joining("\n"));
        } catch (Exception e) {
            throw new RuntimeException("Failed to load prompt: " + resourcePath, e);
        }
    }
    
    public static GeminiPromptRequest buidVisionPrompt(InsightRequest request,
                                                       Map<String, Metadata> metadataMap,
                                                       Map<String, byte[]> imageBytesMap) {
        List<GeminiPromptRequest.Content> contents = new ArrayList<>();
        contents.add(new GeminiPromptRequest.Content(
                "model",
                List.of(new GeminiPromptRequest.Part(SYSTEM_PROMPTS.get(PromptType.VISION)))
        ));

        for (InsightRequest.ImageData image : request.getImages()) {
            Metadata metadata = metadataMap.get(image.getId());
            String time = metadata != null && metadata.getCaptureDate() != null
                    ? metadata.getCaptureDate() : "정보 없음";
            String location = metadata != null && metadata.getLocation() != null
                    && metadata.getLocation().getAddress() != null
                    ? metadata.getLocation().getAddress() : "위치 정보 없음";
            List<String> names = new ArrayList<>();
            if (metadata != null && metadata.getNearbyLandmarks() != null) {
                metadata.getNearbyLandmarks().forEach(lm -> {
                    if (lm.getName() != null && !lm.getName().isEmpty()) {
                        names.add(lm.getName());
                    }
                });
            }
            String landmarks = names.isEmpty() ? "명소 정보 없음" : String.join(", ", names);
            String promptText = String.format("Captured at %s in %s. Landmarks: %s", time, location, landmarks);

            byte[] original = imageBytesMap.get(image.getId());
            byte[] compressed = ImageCompressor.compressImage(original);
            String inlineData = compressed != null
                    ? Base64.getEncoder().encodeToString(compressed) : "";

            GeminiPromptRequest.Part textPart = new GeminiPromptRequest.Part();
            textPart.setText(promptText);
            GeminiPromptRequest.Part imagePart = new GeminiPromptRequest.Part();
            imagePart.setInline_data(new GeminiPromptRequest.InlineData("image/webp", inlineData));

            contents.add(new GeminiPromptRequest.Content("user", List.of(textPart, imagePart)));
        }
        GeminiPromptRequest req = new GeminiPromptRequest();
        req.setContents(contents);
        return req;
    }

    public static GeminiPromptRequest buildDigestPrompt(DigestRequestForPrompt request, ObjectMapper objectMapper) {
        List<GeminiPromptRequest.Content> contents = new ArrayList<>();
        // 다이제스트 시스템 프롬프트 추가
        contents.add(new GeminiPromptRequest.Content(
                "model",
                List.of(new GeminiPromptRequest.Part(SYSTEM_PROMPTS.get(PromptType.DIGEST)))
        ));

        String jsonPayload;
        try {
            jsonPayload = objectMapper.writeValueAsString(request);
        } catch (Exception e) {
            throw new RuntimeException("DigestRequestForPrompt를 JSON으로 변환하는데 실패했습니다", e);
        }

        // JSON 페이로드를 포함한 사용자 부분 추가
        contents.add(new GeminiPromptRequest.Content(
                "user",
                List.of(new GeminiPromptRequest.Part(jsonPayload))
        ));
        
        GeminiPromptRequest req = new GeminiPromptRequest();
        req.setContents(contents);
        return req;
    }
}
