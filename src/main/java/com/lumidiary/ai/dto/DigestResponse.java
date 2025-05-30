package com.lumidiary.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class DigestResponse {
    private String id;
    private Period period;
    private String title;

    private String overallEmotion;
    private String summary;
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
        private String id;
        private String summary;
    }
}

