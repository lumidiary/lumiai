package com.lumidiary.ai.service;

import com.lumidiary.ai.dto.*;
import com.lumidiary.ai.integration.GeminiApiClient;
import com.lumidiary.ai.util.PromptBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DigestService {

    private final GeminiApiClient geminiApiClient;
    private final ObjectMapper objectMapper;

    public DigestResponse createDigest(DigestRequest request) throws IOException {
        // Gemini에게 보낼 요청 객체 생성
        DigestRequestForPrompt requestForPrompt = preparePromptRequest(request);

        // Gemini API 호출
        GeminiPromptRequest prompt = PromptBuilder.buildDigestPrompt(requestForPrompt, objectMapper);
        String jsonResponse = geminiApiClient.sendPromptRaw(prompt);

        // JSON 응답 파싱
        JsonNode responseNode = parseResponseJson(jsonResponse);

        // 최종 응답 조립
        DigestResponse response = assembleResponse(responseNode, request.getEntries());

        // 요청의 ID를 응답에 설정
        response.setId(request.getId());

        return response;
    }

    private DigestRequestForPrompt preparePromptRequest(DigestRequest request) {
        List<DigestEntryForPrompt> entriesForPrompt = new ArrayList<>();

        for (int i = 0; i < request.getEntries().size(); i++) {
            DigestEntry originalEntry = request.getEntries().get(i);

            // 원본 엔트리에서 id, date를 제외한 복사본 생성
            DigestEntryForPrompt entryForPrompt = new DigestEntryForPrompt();
            entryForPrompt.setIndex(i);
            entryForPrompt.setEmotion(originalEntry.getEmotion());

            // 이미지 설명 변환 (위도/경도 정보 제외)
            if (originalEntry.getImageDescriptions() != null) {
                List<DigestEntryForPrompt.ImageDescriptionForPrompt> promptImgDescs = new ArrayList<>();
                for (DigestEntry.ImageDescription originalImgDesc : originalEntry.getImageDescriptions()) {
                    DigestEntryForPrompt.ImageDescriptionForPrompt promptImgDesc = new DigestEntryForPrompt.ImageDescriptionForPrompt();
                    promptImgDesc.setIndex(originalImgDesc.getIndex());
                    promptImgDesc.setDescription(originalImgDesc.getDescription());
                    promptImgDescs.add(promptImgDesc);
                }
                entryForPrompt.setImageDescriptions(promptImgDescs);
            }

            entryForPrompt.setOverallDaySummary(originalEntry.getOverallDaySummary());
            entryForPrompt.setQuestions(originalEntry.getQuestions());

            entriesForPrompt.add(entryForPrompt);
        }

        DigestRequestForPrompt requestForPrompt = new DigestRequestForPrompt();
        requestForPrompt.setEntries(entriesForPrompt);
        requestForPrompt.setUser_locale(request.getUserLocale());

        return requestForPrompt;
    }

    private JsonNode parseResponseJson(String jsonResponse) throws IOException {
        try {
            // Gemini 응답에서 실제 콘텐츠(text) 추출
            JsonNode root = objectMapper.readTree(jsonResponse);
            String rawText = root
                    .path("candidates").get(0)
                    .path("content")
                    .path("parts").get(0)
                    .path("text").asText();

            // JSON 포맷이 코드 블록으로 감싸져 있을 수 있으므로 제거
            String cleaned = rawText
                    .replaceAll("(?s)```json\\s*", "")
                    .replaceAll("```", "").trim();

            System.out.println("원본 Gemini 응답: " + cleaned);

            // 응답 텍스트를 JsonNode로 파싱
            return objectMapper.readTree(cleaned);
        } catch (Exception e) {
            throw new IOException("Failed to parse Gemini response: " + e.getMessage() + "\nFull response: " + jsonResponse, e);
        }
    }

    private DigestResponse assembleResponse(JsonNode responseNode, List<DigestEntry> originalEntries) {
        DigestResponse response = new DigestResponse();

        // 시작일과 종료일을 계산하여 period 설정
        DigestResponse.Period period = calculatePeriod(originalEntries);
        response.setPeriod(period);

        // Gemini로부터 얻은 정보 설정
        if (responseNode.has("title")) {
            response.setTitle(responseNode.get("title").asText());
        } else {
            response.setTitle("다이제스트 요약");
        }

        if (responseNode.has("summary")) {
            response.setSummary(responseNode.get("summary").asText());
        }
        if (responseNode.has("overallEmotion")) {
            response.setOverallEmotion(responseNode.get("overallEmotion").asText());
        }

        // AI Insights 설정
        DigestResponse.AIInsights aiInsights = new DigestResponse.AIInsights();
        JsonNode insightsNode = responseNode.get("aiInsights");
        if (insightsNode != null) {
            if (insightsNode.has("activity")) {
                aiInsights.setActivity(insightsNode.get("activity").asText());
            }

            if (insightsNode.has("emotionTrend")) {
                aiInsights.setEmotionTrend(insightsNode.get("emotionTrend").asText());
            }

            // specialMoment를 문자열로 처리
            if (insightsNode.has("specialMoment")) {
                JsonNode specialMomentNode = insightsNode.get("specialMoment");
                if (specialMomentNode.isObject() && specialMomentNode.has("description")) {
                    // 객체인 경우 description만 사용
                    aiInsights.setSpecialMoment(specialMomentNode.get("description").asText());
                } else if (specialMomentNode.isTextual()) {
                    // 이미 문자열인 경우 그대로 사용
                    aiInsights.setSpecialMoment(specialMomentNode.asText());
                }
            }

            // specialMoments 배열이 있는 경우 (이전 형식 호환성)
            JsonNode specialMomentsNode = insightsNode.get("specialMoments");
            if (specialMomentsNode != null && specialMomentsNode.isArray() && specialMomentsNode.size() > 0) {
                StringBuilder combinedDescription = new StringBuilder();
                for (JsonNode momentNode : specialMomentsNode) {
                    if (momentNode.has("description")) {
                        if (combinedDescription.length() > 0) {
                            combinedDescription.append("\n\n");
                        }
                        combinedDescription.append(momentNode.get("description").asText());
                    }
                }
                aiInsights.setSpecialMoment(combinedDescription.toString());
            }
        }
        response.setAiInsights(aiInsights);

        // entrySummaries 파싱 및 매핑
        Map<Integer, String> entrySummaryMap = new HashMap<>();
        JsonNode entrySummariesNode = responseNode.get("entrySummaries");
        if (entrySummariesNode != null && entrySummariesNode.isArray()) {
            for (JsonNode summaryNode : entrySummariesNode) {
                if (summaryNode.has("index") && summaryNode.has("summary")) {
                    int index = summaryNode.get("index").asInt();
                    String summary = summaryNode.get("summary").asText();
                    entrySummaryMap.put(index, summary);
                }
            }
        }

        // 개별 항목 다이제스트 생성
        List<DigestResponse.EntryDigest> entryDigests = new ArrayList<>();
        for (int i = 0; i < originalEntries.size(); i++) {
            DigestEntry entry = originalEntries.get(i);
            DigestResponse.EntryDigest digest = new DigestResponse.EntryDigest();
            digest.setId(entry.getId());

            // LLM에서 생성된 요약이 있으면 사용, 없으면 overallDaySummary 사용
            String summary = entrySummaryMap.getOrDefault(i, entry.getOverallDaySummary());
            digest.setSummary(summary);

            entryDigests.add(digest);
        }
        response.setEntries(entryDigests);

        return response;
    }

    private DigestResponse.Period calculatePeriod(List<DigestEntry> entries) {
        DigestResponse.Period period = new DigestResponse.Period();

        if (entries == null || entries.isEmpty()) {
            period.setStart("");
            period.setEnd("");
            return period;
        }

        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate earliestDate = null;
        LocalDate latestDate = null;

        for (DigestEntry entry : entries) {
            try {
                if (entry.getDate() != null && !entry.getDate().isEmpty()) {
                    LocalDate entryDate;

                    // ISO 8601 날짜 시간 형식 (2025-03-21T11:05:00+09:00) 처리
                    if (entry.getDate().contains("T")) {
                        // OffsetDateTime, ZonedDateTime, LocalDateTime 시도
                        try {
                            OffsetDateTime offsetDateTime = OffsetDateTime.parse(entry.getDate());
                            entryDate = offsetDateTime.toLocalDate();
                        } catch (DateTimeParseException e1) {
                            try {
                                ZonedDateTime zonedDateTime = ZonedDateTime.parse(entry.getDate());
                                entryDate = zonedDateTime.toLocalDate();
                            } catch (DateTimeParseException e2) {
                                try {
                                    LocalDateTime localDateTime = LocalDateTime.parse(entry.getDate(),
                                            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
                                    entryDate = localDateTime.toLocalDate();
                                } catch (DateTimeParseException e3) {
                                    // ISO 형식이지만 다른 패턴일 수 있음
                                    entryDate = LocalDate.parse(entry.getDate().substring(0, 10));
                                }
                            }
                        }
                    } else {
                        // 단순 날짜 형식 (yyyy-MM-dd)
                        entryDate = LocalDate.parse(entry.getDate(), outputFormatter);
                    }

                    if (earliestDate == null || entryDate.isBefore(earliestDate)) {
                        earliestDate = entryDate;
                    }

                    if (latestDate == null || entryDate.isAfter(latestDate)) {
                        latestDate = entryDate;
                    }
                }
            } catch (Exception e) {
                // 날짜 형식이 잘못된 경우 해당 항목 건너뛰기
                System.err.println("날짜 파싱 오류: " + entry.getDate() + " - " + e.getMessage());
                continue;
            }
        }

        if (earliestDate == null || latestDate == null) {
            period.setStart("");
            period.setEnd("");
            return period;
        }

        period.setStart(earliestDate.format(outputFormatter));
        period.setEnd(latestDate.format(outputFormatter));

        return period;
    }
}
