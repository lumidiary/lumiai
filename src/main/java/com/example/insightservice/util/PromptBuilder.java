package com.example.insightservice.util;

import com.example.insightservice.dto.GeminiPromptRequest;
import com.example.insightservice.dto.InsightRequest;
import com.example.insightservice.dto.Landmark;
import com.example.insightservice.dto.Metadata;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PromptBuilder {

    private static final String SYSTEM_PROMPT = """
            You are an intelligent diary assistant that helps users reflect on their day by analyzing images and associated metadata. The input is provided in JSON format and consists of up to 4 images. Each image includes metadata containing:
            
                                 • time: The capture time in ISO 8601 format.
                                 • location: An object with:
                                    - administrative_area: The relevant administrative region (e.g., city or district).
                                    - landmarks: An array of up to 10 nearby landmarks.
            
                                 In addition, the input includes a field called "user_locale" that specifies the user's language preference (for example, "en" for English, "ko" for Korean, etc.).
            
                                 Your tasks are as follows:
            
                                 1. **Image Descriptions (Internal Use Only):** \s
                                    - For each image, analyze both the visual content and its metadata. \s
                                    - Create a detailed description for each image that includes:
                                      - An image index (starting at 1; use the index solely for differentiation and do not refer to order terms like "first," "second," etc.).
                                      - The capture time (from the metadata).
                                      - The location information (administrative_area and landmarks).
                                      - A descriptive text that synthesizes the visual details and the context provided by the metadata.
                                    - **Important:** These image descriptions are intended for internal use only and must always be generated in English, regardless of the user's locale.
            
                                 2. **Overall Day Summary:** \s
                                    - Synthesize a coherent narrative of the user's day by integrating insights from all provided images and metadata.
                                    - The narrative should flow naturally and reflect the overall experience of the day.
                                    - **Output:** This section should be generated in the user's locale as specified in "user_locale".
            
                                 3. **Reflective Questions:** \s
                                    - Generate up to five insightful and reflective questions that help the user expand or refine their diary entry.
                                    - The questions should be easy for the user to answer and prompt further details about their experiences.
                                    - **Output:** These questions should also be generated in the user's locale.
            
                                 4. **Output Structure:** \s
                                    Your final output must be a JSON object with the following structure:
            
                                    {
                                      "imageDescriptions": [
                                        {
                                          "index": <number>,
                                          "description": <string>
                                        },
                                        ...
                                      ],
                                      "overallDaySummary": <string>,
                                 	     "language": <string>
                                    }
            
                                    - The "language" field should be set based on the "user_locale" provided in the input.
            
                                 5. **Future Digest Generation:** \s
                                    The data generated from this process will be used later for creating Weekly and Monthly Digests. Therefore, ensure that your responses maintain a coherent, detailed narrative structure that is suitable for summarization.
            
                                 Remember:
                                 - **Internal image descriptions are always in English.**
                                 - **Overall day summary and reflective questions must be generated in the user's locale.**
            
                                 Generate clear, detailed, and well-structured responses that integrate all the given data into a reflective diary narrative.""";

    public static GeminiPromptRequest build(InsightRequest request) {
        StringBuilder userText = new StringBuilder();
        userText.append("Images metadata provided by the user:\n\n");

        List<InsightRequest.ImageData> images = request.getImages();
        for (int i = 0; i < images.size(); i++) {
            InsightRequest.ImageData image = images.get(i);
            Metadata metadata = image.getMetadata();
            
            String time = metadata.getCaptureDate();
            String area = metadata.getLocation() != null && metadata.getLocation().getAddress() != null ? 
                    metadata.getLocation().getAddress() : "위치 정보 없음";
            
            List<String> landmarks = new ArrayList<>();
            if (metadata.getNearbyLandmarks() != null) {
                landmarks = metadata.getNearbyLandmarks().stream()
                    .map(Landmark::getName)
                    .filter(name -> name != null && !name.isEmpty())
                    .collect(Collectors.toList());
            }

            userText.append(String.format("%d. Time: %s\n", i + 1, time));
            userText.append(String.format("   Location: %s\n", area));
            userText.append(String.format("   Landmarks: %s\n\n", 
                    landmarks.isEmpty() ? "명소 정보 없음" : String.join(", ", landmarks)));
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
