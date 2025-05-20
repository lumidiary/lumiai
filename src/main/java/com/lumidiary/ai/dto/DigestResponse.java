package com.lumidiary.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class DigestResponse {
    private String id;
    private Period period;
    private String title;
    
    private String overallEmotion;
    private String summary;
    private Statistics statistics;
    private AIInsights aiInsights;
    private List<EntryDigest> entries;
    
    @Data
    public static class Period {
        private String start;
        private String end;
    }
    
    @Data
    public static class Statistics {
        private Map<String, Integer> emotionCounts;
        private List<Location> visitedLocations;
    }
    
    @Data
    public static class Location {
        private double latitude;
        private double longitude;
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
        private String date;
        private String emotion;
        private String summary;
    }
}
