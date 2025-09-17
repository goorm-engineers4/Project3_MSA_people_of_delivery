package com.example.cloudfour.modulecommon.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConfigurationProperties(prefix = "kafka.topics")
public class DlqTopicResolver {
    private Map<String, String> dlqMapping;

    public String resolve(String originalTopic) {
        if (originalTopic == null || originalTopic.isBlank()) {
            throw new IllegalArgumentException("originalTopic must not be null/blank");
        }
        // try symbolic key → DLQ key inference (e.g., paymentEvents -> paymentEventsDLQ)
        if (dlqMapping != null && !dlqMapping.isEmpty()) {
            for (Map.Entry<String, String> e : dlqMapping.entrySet()) {
                if (originalTopic.equals(e.getValue())) {
                    String dlqKey = e.getKey().endsWith("DLQ") ? e.getKey() : e.getKey() + "DLQ";
                    String mapped = dlqMapping.get(dlqKey);
                    if (mapped != null && !mapped.isBlank()) {
                        return mapped;
                    }
                }
            }
        }
        // fallback: suffix-based
        return originalTopic.replaceFirst("\\.v1$", ".dlq.v1");
    }

    public Map<String, String> getDlqMapping() { return dlqMapping; }
    public void setDlqMapping(Map<String, String> dlqMapping) { this.dlqMapping = dlqMapping; }
}
