package com.example.insightservice.service;

import com.example.insightservice.dto.GeminiResponse;
import com.example.insightservice.dto.InsightRequest;
import com.example.insightservice.dto.Metadata;
import com.example.insightservice.integration.GeminiApiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class InsightService {

    private final MetadataService metadataService;
    private final GeminiApiClient geminiApiClient;
    private final RestTemplate restTemplate;

    public GeminiResponse analyze(InsightRequest request) throws Exception {
        Map<String, Metadata> metadataMap = new HashMap<>();
        Map<String, byte[]> imageBytesMap = new HashMap<>();

        for (InsightRequest.ImageData image : request.getImages()) {
            byte[] imageBytes = restTemplate.getForObject(image.getUrl(), byte[].class);
            // 이미지 데이터가 반드시 존재해야 함
            if (imageBytes == null || imageBytes.length == 0) {
                throw new Exception("이미지 데이터 다운로드 실패 for image id: " + image.getId());
            }
            imageBytesMap.put(image.getId(), imageBytes);

            Metadata metadata = metadataService.extractMetadata(imageBytes);
            metadataMap.put(image.getId(), metadata);
        }
        return geminiApiClient.requestToGemini(request, metadataMap, imageBytesMap);
    }
}
