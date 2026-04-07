package com.ll.backend.domain.credit.config;

import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "credit")
public class CreditProperties {

    private String accountUid = "acc_abc123xyz";
    private Long initialBalance;
    private String currency = "KRW";
    private String env = "test";
    private Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
    private Instant updatedAt = Instant.parse("2026-03-01T10:00:00Z");
}
