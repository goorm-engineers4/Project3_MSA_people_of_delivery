package com.example.cloudfour.modulecommon.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConfigurationProperties(prefix = "kafka.topics")
public class DlqTopicResolver {
    private Map<String, String> dlqMapping;

    public String resolve(String originalTopic) {
        if (dlqMapping != null && dlqMapping.containsKey(originalTopic)) {
            return dlqMapping.get(originalTopic);
        }
        if (originalTopic != null && originalTopic.endsWith(".v1")) {
            return originalTopic.substring(0, originalTopic.length() - 3) + ".dlq.v1";
        }
        return originalTopic + ".dlq";
    }

    public Map<String, String> getDlqMapping() { return dlqMapping; }
    public void setDlqMapping(Map<String, String> dlqMapping) { this.dlqMapping = dlqMapping; }
}

