package com.coview.service;

import com.coview.dto.JoinRequestView;
import com.coview.dto.ParticipantView;
import com.coview.dto.RoomSessionResponse;
import com.coview.dto.RoomView;
import com.coview.model.Participant;
import com.coview.model.PendingJoinRequest;
import com.coview.model.PlaybackState;
import com.coview.model.Room;
import com.coview.model.SourceDecision;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Service
public class RoomService {

    private static final Duration ROOM_TTL = Duration.ofHours(12);
    private static final String ROOM_ID_PREFIX = "LJX-";
    private static final String ROOM_ID_ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final Pattern ROOM_ID_PATTERN = Pattern.compile("^LJX-[0-9A-Za-z]{6}$");

    private final SecureRandom random = new SecureRandom();
    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private final Map<String, Map<String, PendingJoinRequest>> joinRequests = new ConcurrentHashMap<>();
    private final VideoModeDetector videoModeDetector;

    public RoomService(VideoModeDetector videoModeDetector) {
        this.videoModeDetector = videoModeDetector;
    }

    public String generateCandidateRoomId() {
        return newRoomId();
    }

    public RoomSessionResponse createRoom(String requestedRoomId, String displayName, String sourceUrl, String password, String confirmPassword) {
        String roomId = normalizeRoomId(requestedRoomId);
        validateRoomId(roomId);
        validatePasswordPair(password, confirmPassword);
        if (rooms.containsKey(roomId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "这个房间号刚刚被占用，请重新生成房间号。");
        }

        SourceDecision sourceDecision = decide(sourceUrl);
        Participant owner = new Participant(newParticipantId(), cleanName(displayName, "房主"), true);
        Room room = new Room(roomId, owner, sourceDecision, hashPassword(password));
        Room existing = rooms.putIfAbsent(roomId, room);
        if (existing != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "这个房间号刚刚被占用，请重新生成房间号。");
        }
        return new RoomSessionResponse(room.getId(), owner.getId(), toView(room));
    }

    public JoinRequestView createJoinRequest(String roomId, String displayName, String password) {
        Room room = requireRoom(roomId);
        if (!passwordMatches(room, password)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "房间密码不正确。");
        }
        PendingJoinRequest request = new PendingJoinRequest(randomToken(18), cleanName(displayName, "房客"));
        joinRequests.computeIfAbsent(room.getId(), ignored -> new ConcurrentHashMap<>()).put(request.getId(), request);
        return toJoinRequestView(room, request, null);
    }

    public JoinRequestView getJoinRequest(String roomId, String requestId) {
        Room room = requireRoom(roomId);
        PendingJoinRequest request = requireJoinRequest(room, requestId);
        RoomView roomView = request.getStatus() == PendingJoinRequest.Status.APPROVED ? toView(room) : null;
        return toJoinRequestView(room, request, roomView);
    }

    public List<JoinRequestView> listPendingJoinRequests(String roomId, String ownerParticipantId) {
        Room room = requireRoom(roomId);
        requireOwner(room, ownerParticipantId);
        return joinRequests.getOrDefault(room.getId(), Map.of())
                .values()
                .stream()
                .filter(request -> request.getStatus() == PendingJoinRequest.Status.PENDING)
                .sorted(Comparator.comparing(PendingJoinRequest::getRequestedAt))
                .map(request -> toJoinRequestView(room, request, null))
                .toList();
    }

    public JoinRequestView decideJoinRequest(String roomId, String ownerParticipantId, String requestId, boolean approved) {
        Room room = requireRoom(roomId);
        requireOwner(room, ownerParticipantId);
        PendingJoinRequest request = requireJoinRequest(room, requestId);
        if (request.getStatus() != PendingJoinRequest.Status.PENDING) {
            RoomView roomView = request.getStatus() == PendingJoinRequest.Status.APPROVED ? toView(room) : null;
            return toJoinRequestView(room, request, roomView);
        }

        if (!approved) {
            request.reject();
            return toJoinRequestView(room, request, null);
        }

        Participant participant = new Participant(newParticipantId(), request.getDisplayName(), false);
        room.addParticipant(participant);
        request.approve(participant.getId());
        return toJoinRequestView(room, request, toView(room));
    }

    public RoomView getRoomView(String roomId) {
        return toView(requireRoom(roomId));
    }

    public Optional<Room> findRoom(String roomId) {
        return Optional.ofNullable(rooms.get(normalizeRoomId(roomId)));
    }

    public RoomView updateSource(String roomId, String participantId, String sourceUrl) {
        Room room = requireRoom(roomId);
        requireOwner(room, participantId);
        room.setSourceDecision(decide(sourceUrl));
        return toView(room);
    }

    public PlaybackState applyPlayback(String roomId, String participantId, String action, double positionSeconds) {
        Room room = requireRoom(roomId);
        Participant participant = room.findParticipant(participantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "\u6210\u5458\u8eab\u4efd\u65e0\u6548\uff0c\u8bf7\u91cd\u65b0\u52a0\u5165\u623f\u95f4\u3002"));
        requireOwner(room, participantId);
        participant.touch();

        PlaybackState next = switch (action == null ? "" : action.toLowerCase(Locale.ROOT)) {
            case "play" -> room.getPlaybackState().withPlayback(true, positionSeconds);
            case "pause" -> room.getPlaybackState().withPlayback(false, positionSeconds);
            case "seek", "state" -> room.getPlaybackState().withPlayback(room.getPlaybackState().playing(), positionSeconds);
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "未知的播放操作。");
        };
        room.setPlaybackState(next);
        return next;
    }

    public RoomView setScreenShareActive(String roomId, String participantId, boolean active) {
        Room room = requireRoom(roomId);
        requireOwner(room, participantId);
        room.findParticipant(participantId).ifPresent(Participant::touch);
        room.setScreenShareActive(active);
        return toView(room);
    }

    public boolean leaveRoom(String roomId, String participantId) {
        Room room = requireRoom(roomId);
        Participant participant = room.findParticipant(participantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "成员身份无效，请重新加入房间。"));
        if (participant.isOwner()) {
            rooms.remove(normalizeRoomId(roomId));
            joinRequests.remove(room.getId());
            return true;
        }
        room.removeParticipant(participantId);
        return false;
    }

    public Participant requireParticipant(String roomId, String participantId) {
        Room room = requireRoom(roomId);
        return room.findParticipant(participantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "成员身份无效，请重新加入房间。"));
    }

    public void touchParticipant(String roomId, String participantId) {
        findRoom(roomId)
                .flatMap(room -> room.findParticipant(participantId))
                .ifPresent(Participant::touch);
    }

    public Room requireRoom(String roomId) {
        Room room = rooms.get(normalizeRoomId(roomId));
        if (room == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "房间不存在或已过期。");
        }
        room.touch();
        return room;
    }

    public boolean isRoomParticipant(String roomId, String participantId) {
        return findRoom(roomId)
                .flatMap(room -> room.findParticipant(participantId))
                .isPresent();
    }

    public RoomView toView(Room room) {
        return new RoomView(
                room.getId(),
                room.getOwnerId(),
                room.getCreatedAt(),
                room.getLastActiveAt(),
                room.getSourceDecision(),
                room.getPlaybackState(),
                room.isScreenShareActive(),
                room.getParticipants().stream()
                        .sorted(Comparator.comparing(Participant::isOwner).reversed()
                                .thenComparing(Participant::getJoinedAt))
                        .map(participant -> new ParticipantView(
                                participant.getId(),
                                participant.getDisplayName(),
                                participant.isOwner(),
                                participant.getJoinedAt(),
                                participant.getLastSeenAt()))
                        .toList()
        );
    }

    @Scheduled(fixedDelay = 600_000)
    public void cleanupExpiredRooms() {
        Instant cutoff = Instant.now().minus(ROOM_TTL);
        rooms.entrySet().removeIf(entry -> entry.getValue().getLastActiveAt().isBefore(cutoff));
    }

    private PendingJoinRequest requireJoinRequest(Room room, String requestId) {
        PendingJoinRequest request = joinRequests.getOrDefault(room.getId(), Map.of()).get(requestId);
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "加入申请不存在或已过期。");
        }
        return request;
    }

    private JoinRequestView toJoinRequestView(Room room, PendingJoinRequest request, RoomView roomView) {
        String message = switch (request.getStatus()) {
            case PENDING -> "等待房主确认。";
            case APPROVED -> "房主已同意，可以进入房间。";
            case REJECTED -> "房主已拒绝本次加入申请。";
        };
        return new JoinRequestView(
                request.getId(),
                room.getId(),
                request.getDisplayName(),
                request.getStatus().name(),
                request.getRequestedAt(),
                request.getParticipantId(),
                roomView,
                message
        );
    }

    private SourceDecision decide(String sourceUrl) {
        try {
            return videoModeDetector.decide(sourceUrl);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    private void requireOwner(Room room, String participantId) {
        if (participantId == null || !room.getOwnerId().equals(participantId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "只有房主可以控制这个房间。");
        }
    }

    private String newRoomId() {
        String id;
        do {
            id = ROOM_ID_PREFIX + randomToken(6);
        } while (rooms.containsKey(normalizeRoomId(id)));
        return id;
    }

    private String newParticipantId() {
        return randomToken(16);
    }

    private String randomToken(int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(ROOM_ID_ALPHABET.charAt(random.nextInt(ROOM_ID_ALPHABET.length())));
        }
        return builder.toString();
    }

    private String cleanName(String displayName, String fallback) {
        if (displayName == null || displayName.isBlank()) {
            return fallback;
        }
        String trimmed = displayName.trim();
        return trimmed.length() > 40 ? trimmed.substring(0, 40) : trimmed;
    }

    private String normalizeRoomId(String roomId) {
        if (roomId == null) {
            return "";
        }
        String clean = roomId.trim();
        if (!clean.toUpperCase(Locale.ROOT).startsWith(ROOM_ID_PREFIX)) {
            clean = ROOM_ID_PREFIX + clean;
        }
        return clean;
    }

    private void validateRoomId(String roomId) {
        if (!ROOM_ID_PATTERN.matcher(roomId).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "房间号格式不正确，请重新生成房间号。");
        }
    }

    private void validatePasswordPair(String password, String confirmPassword) {
        if (password == null || password.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请填写房间密码。");
        }
        if (confirmPassword == null || confirmPassword.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请再次确认房间密码。");
        }
        String first = password.trim();
        String second = confirmPassword.trim();
        if (!first.equals(second)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "两次输入的房间密码不一致。");
        }
        if (first.length() < 4 || first.length() > 32) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "房间密码需要 4 到 32 个字符。");
        }
    }

    private boolean passwordMatches(Room room, String password) {
        if (password == null || password.isBlank()) {
            return false;
        }
        return room.getPasswordHash().equals(hashPassword(password));
    }

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(password.trim().getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte value : bytes) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("当前 Java 环境不支持 SHA-256。", ex);
        }
    }
}
