package com.example.cloudfour.modulecommon.monitoring;

public interface NotificationService {
    void sendAlert(String title, String content);
}