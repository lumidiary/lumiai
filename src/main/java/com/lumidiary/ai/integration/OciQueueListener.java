package com.lumidiary.ai.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumidiary.ai.dto.DigestRequest;
import com.lumidiary.ai.dto.DigestResponse;
import com.lumidiary.ai.dto.GeminiResponse;
import com.lumidiary.ai.dto.VisionRequest;
import com.lumidiary.ai.service.DigestService;
import com.lumidiary.ai.service.VisionService;
import com.oracle.bmc.queue.QueueClient;
import com.oracle.bmc.queue.model.GetMessage;
import com.oracle.bmc.queue.model.MessageMetadata;
import com.oracle.bmc.queue.requests.DeleteMessageRequest;
import com.oracle.bmc.queue.requests.GetMessagesRequest;
import com.oracle.bmc.queue.responses.GetMessagesResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OciQueueListener {

    private final QueueClient queueClient;
    private final VisionService visionService;
    private final DigestService digestService;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    @Value("${oci.queue.ocid}")
    private String queueOcid;

    @Value("${oci.queue.diary.channel-value:diary}")
    private String diaryChannelIdentifier;

    @Value("${oci.queue.diary.callback-url}")
    private String diaryCallbackUrl;

    @Value("${oci.queue.digest.channel-value:digest}")
    private String digestChannelIdentifier;

    @Value("${oci.queue.digest.callback-url}")
    private String digestCallbackUrl;

    @Scheduled(fixedDelayString = "${oci.queue.polling.delay:5000}", initialDelayString = "${oci.queue.polling.initial-delay:1000}")
    public void pollQueue() {
        log.debug("Polling Queue: {}", queueOcid);
        try {
            GetMessagesRequest getMessagesRequest = GetMessagesRequest.builder()
                    .queueId(queueOcid)
                    .visibilityInSeconds(60)
                    .limit(10)
                    .timeoutInSeconds(20)
                    .build();

            GetMessagesResponse response = queueClient.getMessages(getMessagesRequest);
            List<GetMessage> messages = response.getGetMessages().getMessages();

            for (GetMessage message : messages) {
                String channelId = null;
                MessageMetadata metadata = message.getMetadata();
                if (metadata != null) {
                    channelId = metadata.getChannelId();
                }

                String messageContent = message.getContent();
                String receipt = message.getReceipt();
                String messageId = String.valueOf(message.getId());

                log.info("Received message from Queue (ID: {}), Receipt: {}, Determined Channel ID: {}", messageId,
                        receipt, channelId);

                if (channelId == null) {
                    log.warn(
                            "Channel ID could not be determined for message ID: {}. Ensure OciQueueTestController sets it correctly. Skipping message.",
                            messageId);
                    continue;
                }

                try {
                    if (diaryChannelIdentifier.equalsIgnoreCase(channelId)) {
                        log.info("Message (ID: {}) identified for DIARY channel.", messageId);
                        VisionRequest visionRequest = objectMapper.readValue(messageContent, VisionRequest.class);
                        GeminiResponse visionResponse = visionService.analyze(visionRequest);
                        sendCallback(diaryCallbackUrl, visionResponse);
                        log.info("Successfully processed DIARY message ID: {}", messageId);
                    } else if (digestChannelIdentifier.equalsIgnoreCase(channelId)) {
                        log.info("Message (ID: {}) identified for DIGEST channel.", messageId);
                        DigestRequest digestRequest = objectMapper.readValue(messageContent, DigestRequest.class);
                        DigestResponse digestResponse = digestService.createDigest(digestRequest);
                        sendCallback(digestCallbackUrl, digestResponse);
                        log.info("Successfully processed DIGEST message ID: {}", messageId);
                    } else {
                        log.warn("Unknown Channel ID '{}' for message ID: {}. Skipping message.", channelId, messageId);
                    }
                    deleteMessage(queueOcid, receipt);
                } catch (Exception e) {
                    log.error("Error processing message ID: {} (Channel ID: {}): {}", messageId, channelId,
                            e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            log.error("Error polling Queue ({}): {}", queueOcid, e.getMessage(), e);
        }
    }

    private void sendCallback(String callbackUrl, Object payload) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Object> entity = new HttpEntity<>(payload, headers);
            log.info("Sending POST callback to URL: {} with payload size: {}", callbackUrl,
                    objectMapper.writeValueAsString(payload).length());
            restTemplate.postForEntity(callbackUrl, entity, String.class);
            log.info("Successfully sent callback to URL: {}", callbackUrl);
        } catch (Exception e) {
            log.error("Error sending callback to URL {}: {}", callbackUrl, e.getMessage(), e);
        }
    }

    private void deleteMessage(String queueOcid, String messageReceipt) {
        try {
            DeleteMessageRequest deleteMessageRequest = DeleteMessageRequest.builder()
                    .queueId(queueOcid)
                    .messageReceipt(messageReceipt)
                    .build();
            queueClient.deleteMessage(deleteMessageRequest);
            log.info("Successfully deleted message with receipt: {} from queue: {}", messageReceipt, queueOcid);
        } catch (Exception e) {
            log.error("Error deleting message with receipt {} from queue {}: {}", messageReceipt, queueOcid,
                    e.getMessage(), e);
        }
    }
}
