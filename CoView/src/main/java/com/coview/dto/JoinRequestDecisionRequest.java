// 房主处理加入申请的请求参数。
package com.coview.dto;

import jakarta.validation.constraints.NotBlank;

public record JoinRequestDecisionRequest(
        @NotBlank(message = "房主身份缺失") String participantId,
        boolean approved
) {
}
