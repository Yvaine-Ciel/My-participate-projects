package com.coview.model;

import java.time.Instant;

public class Participant {

    private final String id;
    private final String displayName;
    private final boolean owner;
    private final Instant joinedAt;
    private volatile Instant lastSeenAt;

    public Participant(String id, String displayName, boolean owner) {
        this.id = id;
        this.displayName = displayName;
        this.owner = owner;
        this.joinedAt = Instant.now();
        this.lastSeenAt = this.joinedAt;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isOwner() {
        return owner;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }

    public void touch() {
        this.lastSeenAt = Instant.now();
    }
}
