package dev3.nms.util;

import jakarta.servlet.http.HttpSession;

public class SessionUtil {

    public static Long getUserId(HttpSession session) {
        Object userId = session.getAttribute("USER_ID");
        if (userId instanceof Long) return (Long) userId;
        if (userId instanceof Number) return ((Number) userId).longValue();
        return null;
    }
}
