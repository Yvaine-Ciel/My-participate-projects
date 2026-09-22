// 临时观影房间领域模型。
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

    // 初始化房间并把房主加入成员表。
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

    // 按成员 ID 查找房间成员。
    public Optional<Participant> findParticipant(String participantId) {
        return Optional.ofNullable(participants.get(participantId));
    }

    // 添加新成员并刷新房间活跃时间。
    public void addParticipant(Participant participant) {
        participants.put(participant.getId(), participant);
        touch();
    }

    // 移除成员并刷新房间活跃时间。
    public void removeParticipant(String participantId) {
        participants.remove(participantId);
        touch();
    }

    // 更新视频来源并重置播放与共享状态。
    public void setSourceDecision(SourceDecision sourceDecision) {
        this.sourceDecision = sourceDecision;
        this.playbackState = PlaybackState.stopped();
        this.screenShareActive = false;
        touch();
    }

    // 更新播放状态并刷新房间活跃时间。
    public void setPlaybackState(PlaybackState playbackState) {
        this.playbackState = playbackState;
        touch();
    }

    // 更新屏幕共享状态并刷新房间活跃时间。
    public void setScreenShareActive(boolean screenShareActive) {
        this.screenShareActive = screenShareActive;
        touch();
    }

    // 记录房间最近一次活动时间。
    public void touch() {
        this.lastActiveAt = Instant.now();
    }
}
