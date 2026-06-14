package com.mindweather.user.dto;

import lombok.Data;

@Data
public class UserInfoResponse {

    private Long userId;
    private String email;
    private String nickname;
    private String avatar;
    private Boolean defaultAnonymous;
    private String token;
}
