// 房主更换视频来源的请求参数。
package com.coview.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateSourceRequest(
        @NotBlank(message = "成员身份缺失") @Size(max = 64, message = "成员身份无效") String participantId,
        @NotBlank(message = "请填写视频地址") @Size(max = 2048, message = "视频地址不能超过 2048 个字符") String sourceUrl
) {
}
