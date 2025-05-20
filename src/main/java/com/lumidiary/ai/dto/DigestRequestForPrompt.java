package com.lumidiary.ai.dto;

import lombok.Data;
import java.util.List;

@Data
public class DigestRequestForPrompt {
    private List<DigestEntryForPrompt> entries;
    private String user_locale;
}
