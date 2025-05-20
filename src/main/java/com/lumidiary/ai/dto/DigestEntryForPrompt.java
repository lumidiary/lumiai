package com.lumidiary.ai.dto;

import lombok.Data;
import java.util.List;

@Data
public class DigestEntryForPrompt {
    private int index;
    private String emotion;
    private List<ImageDescriptionForPrompt> imageDescriptions;
    private String overallDaySummary;
    private List<DigestEntry.Question> questions;
    
    @Data
    public static class ImageDescriptionForPrompt {
        private int index;
        private String description;
    }
}
