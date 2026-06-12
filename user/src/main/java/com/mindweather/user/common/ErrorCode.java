package com.mindweather.user.common;

public enum ErrorCode {

    SUCCESS(0, "success"),

    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未登录"),
    FORBIDDEN(403, "无权限"),
    NOT_FOUND(404, "资源不存在"),
    SYSTEM_ERROR(500, "系统错误"),

    AI_ANALYSIS_FAILED(1001, "AI分析失败"),
    SENSITIVE_CONTENT(1002, "敏感词违规"),

    EMAIL_ALREADY_EXISTS(2001, "邮箱已注册"),
    USER_NOT_FOUND(2002, "用户不存在"),
    PASSWORD_ERROR(2003, "密码错误"),
    TOKEN_EXPIRED(2004, "Token已过期"),
    TOKEN_INVALID(2005, "Token无效");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
