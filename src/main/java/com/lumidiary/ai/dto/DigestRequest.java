package com.lumidiary.ai.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.util.List;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class) // JSON의 snake_case를 Java camelCase로 자동 매핑
public class DigestRequest {
    private String id;
    private List<DigestEntry> entries;
    private String userLocale; //Java에서는 camelCase
}
