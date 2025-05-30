package com.lumidiary.ai.queue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumidiary.ai.dto.DigestRequest;
import com.lumidiary.ai.dto.VisionRequest;
import com.lumidiary.ai.dto.DigestResponse;
import com.lumidiary.ai.dto.GeminiResponse;
import com.lumidiary.ai.service.DigestService;
import com.lumidiary.ai.service.VisionService;
import com.lumidiary.ai.util.CallbackSender;
import com.oracle.bmc.queue.QueueClient;
import com.oracle.bmc.queue.model.GetMessage;
import com.oracle.bmc.queue.requests.DeleteMessageRequest;
import com.oracle.bmc.queue.requests.GetMessagesRequest;
import com.oracle.bmc.queue.responses.GetMessagesResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "oci.queue.enabled", havingValue = "true")
public class QueueConsumer {

    private final QueueClient queueClient;
    private final DigestService digestService;
    private final VisionService visionService;
    private final CallbackSender callbackSender;
    private final ObjectMapper objectMapper;

    @Value("${oci.queue.id}")
    private String queueId;

    @Value("${callback.digest}")
    private String digestCallbackUrl;

    @Value("${callback.vision}")
    private String visionCallbackUrl;

    @Scheduled(fixedDelay = 5000)
    public void pollQueue() {
        GetMessagesRequest getRequest = GetMessagesRequest.builder()
                .queueId(queueId)
                .limit(5)
                .build();

        GetMessagesResponse response = queueClient.getMessages(getRequest);
        List<GetMessage> messages = response.getGetMessages().getMessages();

        for (GetMessage message : messages) {
            try {
                // 1. 메시지 원본 로그 출력
                log.warn("📦 message.getContent(): [{}]", message.getContent());

                // 2. Base64 디코딩
                String decoded = new String(
                        Base64.getDecoder().decode(message.getContent().getBytes(StandardCharsets.UTF_8)),
                        StandardCharsets.UTF_8
                );
                log.warn("📝 Decoded JSON String: [{}]", decoded);

                // 3. JSON 파싱
                JsonNode jsonNode = objectMapper.readTree(decoded);
                log.debug("🧩 JSON Root Keys: {}", jsonNode.fieldNames());

                // 4. 구조 분기
                if (jsonNode.has("images") && jsonNode.get("images").isArray()) {
                    log.info("🔍 메시지 타입: VisionRequest");
                    VisionRequest visionRequest = objectMapper.treeToValue(jsonNode, VisionRequest.class);
                    GeminiResponse result = visionService.analyze(visionRequest);
                    callbackSender.send(visionCallbackUrl, result);

                } else if (jsonNode.has("entries") && jsonNode.get("entries").isArray()) {
                    log.info("🔍 메시지 타입: DigestRequest");
                    DigestRequest digestRequest = objectMapper.treeToValue(jsonNode, DigestRequest.class);
                    DigestResponse result = digestService.createDigest(digestRequest);
                    callbackSender.send(digestCallbackUrl, result);

                } else {
                    log.warn(" Unknown message structure. JSON root keys: {}", jsonNode.fieldNames());
                }

            } catch (Exception e) {
                log.error(" 메시지 파싱 중 오류 발생 - 원본: {}", message.getContent(), e);
            }

            // 5. 메시지 삭제
            DeleteMessageRequest deleteRequest = DeleteMessageRequest.builder()
                    .queueId(queueId)
                    .messageReceipt(message.getReceipt())
                    .build();
            queueClient.deleteMessage(deleteRequest);
        }
    }
}
