package com.lumidiary.ai.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oracle.bmc.model.BmcException;
import com.oracle.bmc.queue.QueueClient;
import com.oracle.bmc.queue.model.PutMessage;
import com.oracle.bmc.queue.model.PutMessagesDetails;
import com.oracle.bmc.queue.model.PutMessagesDetailsEntry;
import com.oracle.bmc.queue.model.MessageMetadata;
import com.oracle.bmc.queue.requests.PutMessagesRequest;
import com.oracle.bmc.queue.responses.PutMessagesResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/test/queue")
@RequiredArgsConstructor
public class OciQueueTestController {

    private final QueueClient queueClient;
    private final ObjectMapper objectMapper;

    @Value("${oci.queue.ocid}")
    private String queueOcid;

    @Value("${oci.queue.diary.channel-value:diary}")
    private String diaryChannelValue;

    @Value("${oci.queue.digest.channel-value:digest}")
    private String digestChannelValue;

    @PostMapping("/{channelName}")
    public ResponseEntity<String> publishToQueue(
            @PathVariable String channelName,
            @RequestBody Object messagePayload) {

        String targetChannelId;
        if ("diary".equalsIgnoreCase(channelName)) {
            targetChannelId = diaryChannelValue;
        } else if ("digest".equalsIgnoreCase(channelName)) {
            targetChannelId = digestChannelValue;
        } else {
            return ResponseEntity.status(400)
                    .body("Invalid channel name: " + channelName + ". Must be 'diary' or 'digest'.");
        }
        return publishMessage(queueOcid, channelName, targetChannelId, messagePayload);
    }

    private ResponseEntity<String> publishMessage(String effectiveQueueOcid, String channelNameForLog,
            String channelIdForMessage, Object payload) {
        try {
            String messageString = objectMapper.writeValueAsString(payload);

            PutMessagesDetailsEntry entry = PutMessagesDetailsEntry.builder()
                    .content(messageString)
                    .metadata(
                            MessageMetadata.builder()
                                    .channelId(channelIdForMessage)
                                    .build())
                    .build();

            PutMessagesDetails putMessagesDetails = PutMessagesDetails.builder()
                    .messages(Collections.singletonList(entry))
                    .build();

            PutMessagesRequest putMessagesRequest = PutMessagesRequest.builder()
                    .queueId(effectiveQueueOcid)
                    .putMessagesDetails(putMessagesDetails)
                    .build();

            PutMessagesResponse response = queueClient.putMessages(putMessagesRequest);

            com.oracle.bmc.queue.model.PutMessages putMessagesResult = response.getPutMessages();
            List<PutMessage> resultMessages = putMessagesResult.getMessages();

            if (resultMessages != null && !resultMessages.isEmpty()) {
                String messageId = String.valueOf(resultMessages.get(0).getId());
                log.info("Successfully published message to queue {} for channel '{}'. Message ID: {}",
                        effectiveQueueOcid, channelIdForMessage, messageId);
                return ResponseEntity.ok("Message published to channel " + channelNameForLog + ". Message ID: " + messageId);
            } else {
                String errorMessage = "Failed to publish message to channel " + channelNameForLog
                        + ". No messages returned.";
                log.error(errorMessage);
                return ResponseEntity.status(500).body(errorMessage);
            }
        } catch (JsonProcessingException e) {
            log.error("Error serializing message payload for channel {}: {}", channelNameForLog, e.getMessage(), e);
            return ResponseEntity.status(400).body("Error serializing message payload: " + e.getMessage());
        } catch (BmcException e) {
            log.error("OCI service error publishing to channel {}: {}", channelNameForLog, e.getMessage(), e);
            return ResponseEntity.status(e.getStatusCode())
                    .body("OCI service error for channel " + channelNameForLog + ": " + e.getMessage());
        } catch (Exception e) {
            log.error("Error publishing message to channel {}: {}", channelNameForLog, e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body("Error publishing message to channel " + channelNameForLog + " queue: " + e.getMessage());
        }
    }
}
