package com.lumidiary.ai.dto;

import lombok.Data;
import java.util.List;

@Data
public class DigestRequest {
    private String id;
    private List<DigestEntry> entries;
    private String user_locale;
}
