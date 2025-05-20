package com.lumidiary.ai.dto;

import lombok.Data;
import java.util.List;

@Data
public class DigestEntry {
    private String id;
    private String date;
    private String emotion;
    private List<ImageDescription> imageDescriptions;
    private String overallDaySummary;
    private List<Question> questions;

    @Data
    public static class ImageDescription {
        private int index;
        private String description;
        private double latitude;
        private double longitude;
    }

    @Data
    public static class Question {
        private int index;
        private String question;
        private String answer;
    }
}
