package com.example.insightservice.util;

import com.example.insightservice.dto.GeminiPromptRequest;
import com.example.insightservice.dto.InsightRequest;
import com.example.insightservice.dto.Metadata;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

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
                 "images": [
                   {
                     "index": <number>,
                     "description": <string>
                   },
                   ...
                 ],
                 "overallDaySummary": <string>,
                 "questions": [
                   <string>,
                   ...
                 ],
            	 "language": <string>
               }
            
               - The "language" field should be set based on the "user_locale" provided in the input.
            
            5. **Future Digest Generation:** \s
               The data generated from this process will be used later for creating Weekly and Monthly Digests. Therefore, ensure that your responses maintain a coherent, detailed narrative structure that is suitable for summarization.
            
            Remember:
            - **Internal image descriptions are always in English.**
            - **Overall day summary and reflective questions must be generated in the user's locale.**
            
            Generate clear, detailed, and well-structured responses that integrate all the given data into a reflective diary narrative.
            """;

    public static GeminiPromptRequest build(InsightRequest request,
                                              Map<String, Metadata> metadataMap,
                                              Map<String, byte[]> imageBytesMap) {
        List<GeminiPromptRequest.Content> contents = new ArrayList<>();

        contents.add(new GeminiPromptRequest.Content(
                "model",
                List.of(new GeminiPromptRequest.Part(SYSTEM_PROMPT))
        ));

        for (InsightRequest.ImageData image : request.getImages()) {
            Metadata metadata = metadataMap.get(image.getId());
            String time = (metadata != null && metadata.getCaptureDate() != null)
                    ? metadata.getCaptureDate() : "정보 없음";
            String location = (metadata != null && metadata.getLocation() != null 
                    && metadata.getLocation().getAddress() != null)
                    ? metadata.getLocation().getAddress() : "위치 정보 없음";
            List<String> lmList = new ArrayList<>();
            if (metadata != null && metadata.getNearbyLandmarks() != null) {
                metadata.getNearbyLandmarks().forEach(lm -> {
                    if (lm.getName() != null && !lm.getName().isEmpty()) {
                        lmList.add(lm.getName());
                    }
                });
            }
            String landmarks = lmList.isEmpty() ? "명소 정보 없음" : String.join(", ", lmList);
            String promptText = String.format("Captured at %s in %s. Landmarks: %s", time, location, landmarks);

            byte[] originalBytes = imageBytesMap.get(image.getId());
            byte[] compressedBytes = ImageCompressor.compressImage(originalBytes);
            String base64EncodedImage = (compressedBytes != null) ? Base64.getEncoder().encodeToString(compressedBytes) : "";

            GeminiPromptRequest.Part textPart = new GeminiPromptRequest.Part();
            textPart.setText(promptText);

            GeminiPromptRequest.Part inlineDataPart = new GeminiPromptRequest.Part();
            inlineDataPart.setInline_data(new GeminiPromptRequest.InlineData("image/webp", base64EncodedImage));

            List<GeminiPromptRequest.Part> parts = new ArrayList<>();
            parts.add(textPart);
            parts.add(inlineDataPart);

            contents.add(new GeminiPromptRequest.Content("user", parts));
        }
        GeminiPromptRequest prompt = new GeminiPromptRequest();
        prompt.setContents(contents);
        return prompt;
    }
}
