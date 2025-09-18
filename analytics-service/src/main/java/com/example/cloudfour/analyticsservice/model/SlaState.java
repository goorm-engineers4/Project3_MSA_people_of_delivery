package com.example.cloudfour.analyticsservice.model;

import java.time.Instant;

public class SlaState {
    public Instant createdAt;
    public Instant reservedAt;
    public Instant authorizedAt;
    public Instant committedAt;
    public Instant releasedAt;
    public Instant reservationFailedAt;
    public Instant paymentFailedAt;
    public Instant canceledAt;

    public boolean emittedCreatedToReserved;
    public boolean emittedReservedToAuthorized;
    public boolean emittedAuthorizedToCommitted;
    public boolean emittedReservedToReleased;
    public boolean violatedCreatedToReserved;
    public boolean violatedReservedToAuthorized;
    public boolean violatedAuthorizedToCommitted;
    public boolean violatedReservedToReleased;

    public Instant lastUpdatedAt;
    public String storeId;
}
