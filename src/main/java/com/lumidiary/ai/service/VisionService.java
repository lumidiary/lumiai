package com.lumidiary.ai.service;

import com.lumidiary.ai.dto.*;
import com.lumidiary.ai.integration.GeminiApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class VisionService {

    private final MetadataService metadataService;
    private final GeminiApiClient geminiApiClient;
    private final RestTemplate restTemplate;

    public GeminiResponse analyze(VisionRequest request) throws Exception {
        Map<String, Metadata> metadataMap = new HashMap<>();
        Map<String, byte[]> imageBytesMap = new HashMap<>();

        for (VisionRequest.ImageData image : request.getImages()) {
            byte[] imageBytes = restTemplate.getForObject(image.getUrl(), byte[].class);
            if (imageBytes == null || imageBytes.length == 0) {
                throw new Exception("이미지 다운로드 실패: " + image.getId());
            }
            imageBytesMap.put(image.getId(), imageBytes);

            Metadata metadata = metadataService.extractMetadata(imageBytes);
            metadataMap.put(image.getId(), metadata);
        }

        GeminiResponse response = geminiApiClient.requestToGemini(request, metadataMap, imageBytesMap);

        if (response.getLanguage() == null || response.getLanguage().isEmpty()) {
            response.setLanguage(request.getUserLocale() != null ? request.getUserLocale() : "ko");
        }

        if (response.getOverallDaySummary() == null || response.getOverallDaySummary().isEmpty()) {
            response.setOverallDaySummary("오늘 하루의 사진 요약입니다.");
        }

        if (response.getQuestions() == null || response.getQuestions().isEmpty()) {
            response.setQuestions(List.of(
                    "이 사진은 어떤 상황인가요?",
                    "당시 기분은 어땠나요?",
                    "이 순간이 인상 깊었던 이유는?"
            ));
        }

        if (response.getImageDescriptions() == null || response.getImageDescriptions().isEmpty()) {
            List<GeminiResponse.ImageDescription> descriptions = new ArrayList<>();
            for (VisionRequest.ImageData image : request.getImages()) {
                GeminiResponse.ImageDescription desc = new GeminiResponse.ImageDescription();
                desc.setImageId(image.getId());
                desc.setDescription("설명이 제공되지 않았습니다.");
                desc.setMetadata(metadataMap.get(image.getId()));
                descriptions.add(desc);
            }
            response.setImageDescriptions(descriptions);
            response.setImages(descriptions);
        }

        return response;
    }

    public Object process(VisionRequest request) {
        try {
            return this.analyze(request);
        } catch (Exception e) {
            log.error("Vision 분석 중 오류 발생", e);
            throw new RuntimeException("Vision 분석 중 오류 발생: " + e.getMessage(), e);
        }
    }
}
