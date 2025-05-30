package com.lumidiary.ai.config;

import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import com.oracle.bmc.queue.QueueClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class OciQueueConfig {

    @Value("${oci.configFilePath:${user.home}/.oci/config}")
    private String configFilePath;

    @Value("${oci.region}")
    private String region;

    @Bean
    public ConfigFileAuthenticationDetailsProvider authProvider() throws IOException {
        return new ConfigFileAuthenticationDetailsProvider(configFilePath, "DEFAULT");
    }

    @Bean
    public QueueClient queueClient(ConfigFileAuthenticationDetailsProvider authProvider) {
        // region 예: "ap-chuncheon-1"
        // cell-1 전용 엔드포인트를 자동으로 구성합니다.
        String queueEndpoint = String.format(
                "https://cell-1.queue.messaging.%s.oci.oraclecloud.com",
                region
        );

        return QueueClient.builder()
                .endpoint(queueEndpoint)   // 자동 생성된 endpoint
                .build(authProvider);
    }
}
