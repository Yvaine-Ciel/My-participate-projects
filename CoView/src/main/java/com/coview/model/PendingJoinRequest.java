package com.coview.model;

import java.time.Instant;

public class PendingJoinRequest {

    public enum Status {
        PENDING,
        APPROVED,
        REJECTED
    }

    private final String id;
    private final String displayName;
    private final Instant requestedAt;
    private volatile Status status;
    private volatile String participantId;
    private volatile Instant decidedAt;

    public PendingJoinRequest(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
        this.requestedAt = Instant.now();
        this.status = Status.PENDING;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public Status getStatus() {
        return status;
    }

    public String getParticipantId() {
        return participantId;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }

    public void approve(String approvedParticipantId) {
        this.status = Status.APPROVED;
        this.participantId = approvedParticipantId;
        this.decidedAt = Instant.now();
    }

    public void reject() {
        this.status = Status.REJECTED;
        this.decidedAt = Instant.now();
    }
}
