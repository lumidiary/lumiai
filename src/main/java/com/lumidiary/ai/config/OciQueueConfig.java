package com.lumidiary.ai.config;

import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.queue.QueueClient;
import com.oracle.bmc.queue.requests.GetQueueRequest;
import com.oracle.bmc.queue.responses.GetQueueResponse;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class OciQueueConfig {

    @Getter
    private QueueClient queueClient;

    private static final String CONFIG_PATH = System.getProperty("user.home") + "/.oci/config";
    private static final String PROFILE = "DEFAULT";

    @PostConstruct
    public void init() {
        try {
            log.info("OCI QueueClient 초기화 시작 - configFile: {}, profile: {}", CONFIG_PATH, PROFILE);
            AuthenticationDetailsProvider provider = new ConfigFileAuthenticationDetailsProvider(CONFIG_PATH, PROFILE);

            queueClient = new QueueClient(provider);
            //  여기서 cell-1 엔드포인트를 명시적으로 설정
            queueClient.setEndpoint("https://cell-1.queue.messaging.ap-chuncheon-1.oci.oraclecloud.com");

            log.info("QueueClient 초기화 성공 - Endpoint: {}", queueClient.getEndpoint());
        } catch (Exception e) {
            log.error("QueueClient 초기화 실패", e);
        }
    }

    @Bean
    public QueueClient queueClient() {
        return queueClient;
    }
}
