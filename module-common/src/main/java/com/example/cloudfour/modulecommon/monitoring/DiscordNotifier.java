package com.example.cloudfour.modulecommon.monitoring;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class DiscordNotifier implements NotificationService {

    @Value("${alert.discord.webhookUrl}")
    private String webhookUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public void sendAlert(String title, String content) {
        try {
            if (webhookUrl == null || webhookUrl.isBlank()) {
                log.debug("Discord Webhook URL이 설정되지 않아 알림을 생략합니다.");
                return;
            }
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String payload = buildPayload(title, content);
            HttpEntity<String> req = new HttpEntity<>(payload, headers);
            restTemplate.postForEntity(webhookUrl, req, String.class);
        } catch (Exception e) {
            log.error("Discord 알림 전송 실패: {}", e.getMessage(), e);
        }
    }

    private String buildPayload(String title, String content) {
        String msg = (title != null ? "**" + title + "**\n" : "") + content;
        return "{\"content\":" + jsonEscape(msg) + "}";
    }

    private String jsonEscape(String s) {
        String esc = s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
        return "\"" + esc + "\"";
    }
}
