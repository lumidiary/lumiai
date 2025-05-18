package com.lumidiary.ai.service;

import com.lumidiary.ai.dto.GeminiResponse;
import com.lumidiary.ai.dto.InsightRequest;
import com.lumidiary.ai.dto.Metadata;
import com.lumidiary.ai.integration.GeminiApiClient;
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
        GeminiResponse response = geminiApiClient.requestToGemini(request, metadataMap, imageBytesMap);
        
        // 요청 시 이미지 순서대로 각 이미지 설명에 ID와 metadata 추가
        for (int i = 0; i < request.getImages().size() && i < response.getImages().size(); i++) {
            InsightRequest.ImageData image = request.getImages().get(i);
            GeminiResponse.ImageDescription imgDesc = response.getImages().get(i);
            imgDesc.setImageId(image.getId());
            imgDesc.setMetadata(metadataMap.get(image.getId()));
        }
        return response;
    }
}
