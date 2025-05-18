package com.lumidiary.ai.util;

import com.lumidiary.ai.dto.GeminiPromptRequest;
import com.lumidiary.ai.dto.InsightRequest;
import com.lumidiary.ai.dto.Metadata;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PromptBuilder {

    private static final String SYSTEM_PROMPT = loadSystemPrompt();

    private static String loadSystemPrompt() {
        try (InputStream input = PromptBuilder.class.getResourceAsStream("/prompts/vision.txt")) {
            if (input == null) {
                throw new RuntimeException("vision.txt 리소스를 찾을 수 없습니다.");
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        } catch (Exception e) {
            throw new RuntimeException("시스템 프롬프트 로딩 중 에러 발생", e);
        }
    }

    public static GeminiPromptRequest build(InsightRequest request,
                                              Map<String, Metadata> metadataMap,
                                              Map<String, byte[]> imageBytesMap) {
        List<GeminiPromptRequest.Content> contents = new ArrayList<>();

        contents.add(new GeminiPromptRequest.Content(
                "model",
                List.of(new GeminiPromptRequest.Part(SYSTEM_PROMPT))
        ));

        for (InsightRequest.ImageData image : request.getImages()) {
            Metadata metadata = metadataMap.get(image.getId());
            String time = (metadata != null && metadata.getCaptureDate() != null)
                    ? metadata.getCaptureDate() : "정보 없음";
            String location = (metadata != null && metadata.getLocation() != null 
                    && metadata.getLocation().getAddress() != null)
                    ? metadata.getLocation().getAddress() : "위치 정보 없음";
            List<String> lmList = new ArrayList<>();
            if (metadata != null && metadata.getNearbyLandmarks() != null) {
                metadata.getNearbyLandmarks().forEach(lm -> {
                    if (lm.getName() != null && !lm.getName().isEmpty()) {
                        lmList.add(lm.getName());
                    }
                });
            }
            String landmarks = lmList.isEmpty() ? "명소 정보 없음" : String.join(", ", lmList);
            String promptText = String.format("Captured at %s in %s. Landmarks: %s", time, location, landmarks);

            byte[] originalBytes = imageBytesMap.get(image.getId());
            byte[] compressedBytes = ImageCompressor.compressImage(originalBytes);
            String base64EncodedImage = (compressedBytes != null) ? Base64.getEncoder().encodeToString(compressedBytes) : "";

            GeminiPromptRequest.Part textPart = new GeminiPromptRequest.Part();
            textPart.setText(promptText);

            GeminiPromptRequest.Part inlineDataPart = new GeminiPromptRequest.Part();
            inlineDataPart.setInline_data(new GeminiPromptRequest.InlineData("image/webp", base64EncodedImage));

            List<GeminiPromptRequest.Part> parts = new ArrayList<>();
            parts.add(textPart);
            parts.add(inlineDataPart);

            contents.add(new GeminiPromptRequest.Content("user", parts));
        }
        GeminiPromptRequest prompt = new GeminiPromptRequest();
        prompt.setContents(contents);
        return prompt;
    }
}
