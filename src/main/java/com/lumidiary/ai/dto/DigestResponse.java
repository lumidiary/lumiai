package com.lumidiary.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class DigestResponse {
    private UUID id;  // String → UUID로 변경
    private Period period;
    private String title;

    private String overallEmotion;

    @JsonProperty("summary")  // lumicore의 digestSummary와 매핑
    private String digestSummary;  // summary → digestSummary로 변경

    private AIInsights aiInsights;
    private List<EntryDigest> entries;

    @Data
    public static class Period {
        private String start;
        private String end;
    }

    @Data
    public static class AIInsights {
        private String activity;
        private String emotionTrend;
        private String specialMoment;
    }

    @Data
    public static class EntryDigest {
        private UUID id;  // String → UUID로 변경
        private String summary;
    }
}