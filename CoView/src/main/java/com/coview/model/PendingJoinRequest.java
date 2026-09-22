// 待房主确认的加入申请模型。
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

    // 创建待房主确认的加入申请。
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

    // 标记申请通过并绑定正式成员 ID。
    public void approve(String approvedParticipantId) {
        this.status = Status.APPROVED;
        this.participantId = approvedParticipantId;
        this.decidedAt = Instant.now();
    }

    // 标记申请被拒绝。
    public void reject() {
        this.status = Status.REJECTED;
        this.decidedAt = Instant.now();
    }
}
