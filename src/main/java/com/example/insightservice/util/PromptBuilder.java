package com.example.insightservice.util;

import com.example.insightservice.dto.GeminiPromptRequest;
import com.example.insightservice.dto.InsightRequest;

import java.util.ArrayList;
import java.util.List;

public class PromptBuilder {

    private static final String SYSTEM_PROMPT = """
You are an intelligent diary assistant that helps users reflect on their day by analyzing images and associated metadata. The input is provided in JSON format and consists of up to 4 images. Each image includes metadata containing:

• time: The capture time in ISO 8601 format.
• location: An object with:
   - administrative_area: The relevant administrative region (e.g., city or district).
   - landmarks: An array of up to 10 nearby landmarks.

In addition, the input includes a field called "user_locale" that specifies the user's language preference (e.g., "ko" or "en").

Your tasks are as follows:

1. Image Descriptions (Internal Use Only):
   - Analyze each image metadata.
   - Generate a description in English.

2. Overall Day Summary:
   - Write a summary of the day in the user's locale.

3. Reflective Questions:
   - Generate 3 to 5 diary-reflective questions in the user's locale.

4. Output must be returned in this JSON structure:

{
  "imageDescriptions": [
    { "index": 1, "description": "..." }
  ],
  "overallDaySummary": "...",
  "questions": ["...", "..."],
  "language": "ko"
}

✅ Respond strictly in JSON format. Do not include code blocks, markdown, or explanations.
""";

    public static GeminiPromptRequest build(InsightRequest request) {
        StringBuilder userText = new StringBuilder();
        userText.append("Images metadata provided by the user:\n\n");

        List<InsightRequest.ImageData> images = request.getImages();
        for (int i = 0; i < images.size(); i++) {
            InsightRequest.ImageData image = images.get(i);
            InsightRequest.ImageData.Metadata metadata = image.getMetadata();
            String time = metadata.getTime();
            String area = metadata.getLocation().getAdministrative_area();
            List<String> landmarks = metadata.getLocation().getLandmarks();

            userText.append(String.format("%d. Time: %s\n", i + 1, time));
            userText.append(String.format("   Location: %s\n", area));
            userText.append(String.format("   Landmarks: %s\n\n", String.join(", ", landmarks)));
        }

        userText.append("User's preferred language: ").append(request.getUser_locale());

        List<GeminiPromptRequest.Part> parts = new ArrayList<>();
        parts.add(new GeminiPromptRequest.Part(SYSTEM_PROMPT));
        parts.add(new GeminiPromptRequest.Part(userText.toString()));

        List<GeminiPromptRequest.Content> contents = new ArrayList<>();
        contents.add(new GeminiPromptRequest.Content("user", parts));

        GeminiPromptRequest prompt = new GeminiPromptRequest();
        prompt.setContents(contents);
        return prompt;
    }
}