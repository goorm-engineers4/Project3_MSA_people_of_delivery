package com.example.cloudfour.analyticsservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sla.thresholds")
public class SlaThresholds {
    private long createdToReservedMs = 10 * 60 * 1000L;
    private long reservedToAuthorizedMs = 5 * 60 * 1000L;
    private long authorizedToCommittedMs = 10 * 60 * 1000L;
    private long reservedToReleasedMs = 10 * 60 * 1000L;

    public long getCreatedToReservedMs() { return createdToReservedMs; }
    public void setCreatedToReservedMs(long v) { this.createdToReservedMs = v; }

    public long getReservedToAuthorizedMs() { return reservedToAuthorizedMs; }
    public void setReservedToAuthorizedMs(long v) { this.reservedToAuthorizedMs = v; }

    public long getAuthorizedToCommittedMs() { return authorizedToCommittedMs; }
    public void setAuthorizedToCommittedMs(long v) { this.authorizedToCommittedMs = v; }

    public long getReservedToReleasedMs() { return reservedToReleasedMs; }
    public void setReservedToReleasedMs(long v) { this.reservedToReleasedMs = v; }
}
