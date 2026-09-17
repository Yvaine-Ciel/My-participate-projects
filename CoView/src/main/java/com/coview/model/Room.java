package com.coview.model;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class Room {

    private final String id;
    private final String ownerId;
    private final String passwordHash;
    private final Instant createdAt;
    private final ConcurrentMap<String, Participant> participants;
    private volatile Instant lastActiveAt;
    private volatile SourceDecision sourceDecision;
    private volatile PlaybackState playbackState;
    private volatile boolean screenShareActive;

    public Room(String id, Participant owner, SourceDecision sourceDecision, String passwordHash) {
        this.id = id;
        this.ownerId = owner.getId();
        this.passwordHash = passwordHash;
        this.createdAt = Instant.now();
        this.lastActiveAt = this.createdAt;
        this.participants = new ConcurrentHashMap<>();
        this.participants.put(owner.getId(), owner);
        this.sourceDecision = sourceDecision;
        this.playbackState = PlaybackState.stopped();
        this.screenShareActive = false;
    }

    public String getId() {
        return id;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastActiveAt() {
        return lastActiveAt;
    }

    public SourceDecision getSourceDecision() {
        return sourceDecision;
    }

    public PlaybackState getPlaybackState() {
        return playbackState;
    }

    public boolean isScreenShareActive() {
        return screenShareActive;
    }

    public Collection<Participant> getParticipants() {
        return participants.values();
    }

    public Optional<Participant> findParticipant(String participantId) {
        return Optional.ofNullable(participants.get(participantId));
    }

    public void addParticipant(Participant participant) {
        participants.put(participant.getId(), participant);
        touch();
    }

    public void removeParticipant(String participantId) {
        participants.remove(participantId);
        touch();
    }

    public void setSourceDecision(SourceDecision sourceDecision) {
        this.sourceDecision = sourceDecision;
        this.playbackState = PlaybackState.stopped();
        this.screenShareActive = false;
        touch();
    }

    public void setPlaybackState(PlaybackState playbackState) {
        this.playbackState = playbackState;
        touch();
    }

    public void setScreenShareActive(boolean screenShareActive) {
        this.screenShareActive = screenShareActive;
        touch();
    }

    public void touch() {
        this.lastActiveAt = Instant.now();
    }
}
