package com.lumidiary.ai.config;

import com.oracle.bmc.Region;
import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.auth.SimpleAuthenticationDetailsProvider;
import com.oracle.bmc.queue.QueueClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

@Slf4j
@Configuration
public class OciQueueConfig {

    @Value("${oci.user-ocid}")
    private String userOcid;

    @Value("${oci.tenancy-ocid}")
    private String tenancyOcid;

    @Value("${oci.api-key-fingerprint}")
    private String apiKeyFingerprint;

    @Value("${oci.private-key}")
    private String privateKeyContent;

    @Value("${oci.region}")
    private String regionStr;

    @Value("${oci.queue.endpoint}")
    private String queueEndpoint;

    @Bean
    public AuthenticationDetailsProvider authenticationDetailsProvider() throws IOException {
        log.info("Creating OCI AuthenticationDetailsProvider.");
        log.info("User OCID: {}, Tenancy OCID: {}, Fingerprint: {}, Region: {}",
                userOcid, tenancyOcid, apiKeyFingerprint, regionStr);

        Supplier<InputStream> privateKeySupplier = () -> {
            if (privateKeyContent == null || privateKeyContent.isEmpty()) {
                throw new IllegalStateException("Environment variable OCI_PRIVATE_KEY must be set");
            }
            return new ByteArrayInputStream(privateKeyContent.getBytes(StandardCharsets.UTF_8));
        };

        return SimpleAuthenticationDetailsProvider.builder()
                .userId(userOcid)
                .fingerprint(apiKeyFingerprint)
                .tenantId(tenancyOcid)
                .privateKeySupplier(privateKeySupplier)
                .region(Region.valueOf(regionStr.toUpperCase()))
                .build();
    }

    @Bean(destroyMethod = "close")
    public QueueClient queueClient(AuthenticationDetailsProvider provider) {
        QueueClient queueClient = QueueClient.builder()
                .region(Region.valueOf(regionStr.toUpperCase()))
                .build(provider);
        queueClient.setEndpoint(queueEndpoint);
        log.info("OCI QueueClient created for region: {} with endpoint: {}", regionStr, queueEndpoint);
        return queueClient;
    }
}
