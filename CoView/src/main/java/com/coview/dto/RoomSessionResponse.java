// 创建或加入房间后的会话响应。
package com.coview.dto;

public record RoomSessionResponse(
        String roomId,
        String participantId,
        RoomView room
) {
}
