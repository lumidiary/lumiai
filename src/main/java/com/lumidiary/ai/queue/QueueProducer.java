package com.lumidiary.ai.queue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oracle.bmc.queue.QueueClient;
import com.oracle.bmc.queue.model.PutMessagesDetails;
import com.oracle.bmc.queue.model.PutMessagesDetailsEntry;
import com.oracle.bmc.queue.requests.PutMessagesRequest;
import com.oracle.bmc.queue.responses.PutMessagesResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class QueueProducer {

    private final QueueClient queueClient;
    private final ObjectMapper objectMapper;

    @Value("${oci.queue.id}")
    private String queueId;

    // ✅ DTO → JSON → base64 로 전송 (정상)
    public void send(Object request) {
        try {
            String json = objectMapper.writeValueAsString(request);
            sendRaw(json); // 직렬화된 JSON을 Raw 방식으로 전달
        } catch (Exception e) {
            log.error("[QueueProducer] DTO 직렬화 실패", e);
        }
    }

    // ✅ JSON 문자열 → base64 그대로 전송 (직렬화 X)
    public void sendRaw(String json) {
        try {
            String base64 = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));

            PutMessagesDetailsEntry messageEntry = PutMessagesDetailsEntry.builder()
                    .content(base64)
                    .build();

            PutMessagesRequest putRequest = PutMessagesRequest.builder()
                    .queueId(queueId)
                    .putMessagesDetails(PutMessagesDetails.builder()
                            .messages(List.of(messageEntry))
                            .build())
                    .build();

            PutMessagesResponse response = queueClient.putMessages(putRequest);
            log.info("[QueueProducer] 메시지 전송 성공 (Raw): {}", json);

        } catch (Exception e) {
            log.error("[QueueProducer] 메시지 전송 실패 (Raw)", e);
        }
    }
}
