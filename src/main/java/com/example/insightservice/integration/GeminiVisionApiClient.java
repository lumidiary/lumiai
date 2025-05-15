package com.example.insightservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class GeminiVisionApiClient {

    @Value("[REDACTED]")
    private String apiKey;


    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash-001:generateContent?key=";

    public String analyzeImageWithPrompt(MultipartFile imageFile, String promptText) throws IOException {
        // Base64로 인코딩된 이미지
        String base64Image = Base64.getEncoder().encodeToString(imageFile.getBytes());

        // 요청 JSON 생성
        String json = """
        {
          "contents": [
            {
              "role": "user",
              "parts": [
                {
                  "inline_data": {
                    "mime_type": "image/jpeg",
                    "data": "%s"
                  }
                },
                {
                  "text": "%s"
                }
              ]
            }
          ]
        }
        """.formatted(base64Image, promptText.replace("\"", "\\\""));

        // OkHttpClient에 타임아웃 설정 추가
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        RequestBody body = RequestBody.create(json, MediaType.get("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url(GEMINI_URL + apiKey)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Gemini API 호출 실패: " + response);
            }
            return response.body().string();
        }
    }
}
