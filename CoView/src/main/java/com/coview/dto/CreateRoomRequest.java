package com.coview.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateRoomRequest(
        @NotBlank(message = "房间号缺失，请重新生成房间号")
        @Pattern(regexp = "^LJX-[0-9A-Za-z]{6}$", message = "房间号格式不正确")
        String roomId,
        @Size(max = 40, message = "用户名最多 40 个字符")
        String displayName,
        @NotBlank(message = "请填写视频地址")
        @Size(max = 2048, message = "视频地址不能超过 2048 个字符")
        String sourceUrl,
        @NotBlank(message = "请填写房间密码")
        @Size(min = 4, max = 32, message = "房间密码需要 4 到 32 个字符")
        String password,
        @NotBlank(message = "请再次确认房间密码")
        @Size(min = 4, max = 32, message = "确认密码需要 4 到 32 个字符")
        String confirmPassword
) {
}
