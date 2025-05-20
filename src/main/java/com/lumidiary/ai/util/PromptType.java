package com.lumidiary.ai.util;

import lombok.Getter;

@Getter
public enum PromptType {
    VISION("/prompts/vision.txt"),
    DIGEST("/prompts/digest.txt");

    private final String path;

    PromptType(String path) {
        this.path = path;
    }

}
