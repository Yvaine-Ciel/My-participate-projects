package com.coview.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JoinRoomRequest(
        @NotBlank(message = "请填写昵称") @Size(max = 40, message = "昵称最多 40 个字符") String displayName,
        @NotBlank(message = "请输入房间密码") @Size(max = 32, message = "房间密码不能超过 32 个字符") String password
) {
}
