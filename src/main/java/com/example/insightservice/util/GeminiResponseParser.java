package com.example.insightservice.util;

import java.util.*;

public class GeminiResponseParser {

    public static Map<String, Object> parse(String geminiRawText) {
        Map<String, Object> result = new HashMap<>();

        List<String> imageDescriptions = new ArrayList<>();
        List<String> questions = new ArrayList<>();
        StringBuilder summary = new StringBuilder();

        String[] lines = geminiRawText.split("\\n");

        boolean inImageDescription = false;
        boolean inSummary = false;
        boolean inQuestions = false;

        for (String line : lines) {
            String trimmed = line.trim();

            // 구간 시작 라벨 구분
            if (trimmed.matches("(?i)^image description(s)?[:\\-]?.*")) {
                inImageDescription = true;
                inSummary = false;
                inQuestions = false;
                continue;
            } else if (trimmed.matches("(?i)^summary[:\\-]?.*")) {
                inImageDescription = false;
                inSummary = true;
                inQuestions = false;
                continue;
            } else if (trimmed.matches("(?i)^question(s)?[:\\-]?.*")) {
                inImageDescription = false;
                inSummary = false;
                inQuestions = true;
                continue;
            }

            // 내용 추가
            if (inImageDescription && !trimmed.isBlank()) {
                imageDescriptions.add(trimmed);
            } else if (inSummary && !trimmed.isBlank()) {
                summary.append(trimmed).append(" ");
            } else if (inQuestions && !trimmed.isBlank()) {
                // 번호나 하이픈 제거
                questions.add(trimmed.replaceAll("^[\\-\\d.\\)]\\s*", ""));
            }
        }

        result.put("imageDescriptions", imageDescriptions);
        result.put("overallDaySummary", summary.toString().trim());
        result.put("questions", questions);
        return result;
    }
}

