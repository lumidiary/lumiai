package com.lumidiary.ai.queue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumidiary.ai.dto.*;
import com.lumidiary.ai.service.DigestService;
import com.lumidiary.ai.service.VisionService;
import com.oracle.bmc.queue.QueueClient;
import com.oracle.bmc.queue.model.GetMessage;
import com.oracle.bmc.queue.requests.DeleteMessageRequest;
import com.oracle.bmc.queue.requests.GetMessagesRequest;
import com.oracle.bmc.queue.responses.GetMessagesResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@ConditionalOnProperty(name = "oci.queue.enabled", havingValue = "true", matchIfMissing = false)
@Slf4j
public class QueueConsumer {

    private final QueueClient queueClient;
    private final DigestService digestService;
    private final VisionService visionService;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    @Value("${oci.queue.id}")
    private String queueId;

    @Value("${oci.queue.channels.default:diary}")
    private String defaultChannel;

    @Value("${oci.queue.channels.digest:digest}")
    private String digestChannel;

    @Value("${callback.digest}")
    private String digestCallbackUrl;

    @Value("${callback.diary}")
    private String diaryCallbackUrl;

    public QueueConsumer(QueueClient queueClient, DigestService digestService,
                         VisionService visionService, ObjectMapper objectMapper) {
        this.queueClient = queueClient;
        this.digestService = digestService;
        this.visionService = visionService;
        this.objectMapper = objectMapper;
        this.restTemplate = new RestTemplate();
    }

    @Scheduled(fixedDelay = 5000) // 5초마다 실행
    public void consumeMessages() {
        try {
            log.debug("Queue 메시지 확인 시작...");

            GetMessagesRequest getMessagesRequest = GetMessagesRequest.builder()
                    .queueId(queueId)
                    .limit(10) // 한 번에 가져올 메시지 수
                    .timeoutInSeconds(30)
                    .build();

            GetMessagesResponse response = queueClient.getMessages(getMessagesRequest);

            if (response.getGetMessages() != null && response.getGetMessages().getMessages() != null) {
                for (GetMessage message : response.getGetMessages().getMessages()) {
                    processMessage(message);
                    deleteMessage(message.getReceipt());
                }
            }

        } catch (Exception e) {
            log.error("메시지 소비 중 오류 발생: ", e);
        }
    }

    private void processMessage(GetMessage message) {
        try {
            String messageContent = message.getContent();
            log.info("메시지 처리 시작: {}", messageContent);

            // 메시지에서 채널 정보 추출
            QueueMessage queueMessage = parseQueueMessage(messageContent);
            String channel = queueMessage.getChannel();
            String requestData = queueMessage.getData();

            Object result;
            String callbackUrl;

            // 채널별 서비스 처리
            switch (channel.toLowerCase()) {
                case "diary":
                    VisionRequest visionRequest = objectMapper.readValue(requestData, VisionRequest.class);
                    result = visionService.process(visionRequest);
                    callbackUrl = diaryCallbackUrl;
                    break;

                case "digest":
                    DigestRequest digestRequest = objectMapper.readValue(requestData, DigestRequest.class);
                    result = digestService.process(digestRequest);
                    callbackUrl = digestCallbackUrl;
                    break;

                default:
                    log.warn("알 수 없는 채널: {}. 메시지를 건너뜁니다.", channel);
                    return;
            }

            // 콜백 전송
            sendCallback(callbackUrl, result);

            log.info("메시지 처리 완료 - Channel: {}, Callback: {}", channel, callbackUrl);

        } catch (Exception e) {
            log.error("메시지 처리 중 오류 발생 - Message ID: {}, Error: ", message.getId(), e);
        }
    }

    private QueueMessage parseQueueMessage(String messageContent) throws Exception {
        try {
            // 먼저 QueueMessage 형태로 파싱 시도
            return objectMapper.readValue(messageContent, QueueMessage.class);
        } catch (Exception e) {
            // 실패하면 기본 채널(diary)로 가정하고 전체를 data로 처리
            log.info("QueueMessage 형태가 아님. 기본 채널(diary)로 처리: {}", e.getMessage());
            QueueMessage queueMessage = new QueueMessage();
            queueMessage.setChannel("diary");
            queueMessage.setData(messageContent);
            return queueMessage;
        }
    }

    private void sendCallback(String callbackUrl, Object result) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            String jsonResult = objectMapper.writeValueAsString(result);
            HttpEntity<String> request = new HttpEntity<>(jsonResult, headers);

            log.info("콜백 전송 시작 - URL: {}", callbackUrl);
            restTemplate.postForEntity(callbackUrl, request, String.class);
            log.info("콜백 전송 완료 - URL: {}", callbackUrl);

        } catch (Exception e) {
            log.error("콜백 전송 실패 - URL: {}, Error: ", callbackUrl, e);
            // TODO: 재시도 로직 구현 (여유 있을 때)
        }
    }

    private void deleteMessage(String receipt) {
        try {
            DeleteMessageRequest deleteRequest = DeleteMessageRequest.builder()
                    .queueId(queueId)
                    .messageReceipt(receipt)
                    .build();

            queueClient.deleteMessage(deleteRequest);
            log.debug("메시지 삭제 완료: {}", receipt);

        } catch (Exception e) {
            log.error("메시지 삭제 중 오류 발생: ", e);
        }
    }

    // 큐 메시지 구조를 정의하는 내부 클래스
    private static class QueueMessage {
        private String channel;
        private String data;

        public String getChannel() { return channel; }
        public void setChannel(String channel) { this.channel = channel; }
        public String getData() { return data; }
        public void setData(String data) { this.data = data; }
    }
}