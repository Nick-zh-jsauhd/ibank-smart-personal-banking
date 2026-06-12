package com.bank.util;

import javax.servlet.http.HttpServletRequest;

public final class RequestUtil {
    private RequestUtil() {
    }

    public static String trim(HttpServletRequest request, String name) {
        String value = request.getParameter(name);
        return value == null ? "" : value.trim();
    }

    public static String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && forwardedFor.trim().length() > 0) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    public static String userAgent(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null) {
            return "";
        }
        return userAgent.length() > 255 ? userAgent.substring(0, 255) : userAgent;
    }
}
