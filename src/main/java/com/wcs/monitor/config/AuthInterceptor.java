package com.wcs.monitor.config;

import com.wcs.monitor.common.TokenStore;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    public static final String SESSION_ATTR = "SESSION_USER";

    private final TokenStore tokenStore;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String authorization = request.getHeader("Authorization");
        TokenStore.Session session = null;
        if (authorization != null && authorization.startsWith("Bearer ")) {
            session = tokenStore.get(authorization.substring(7));
        }
        if (session == null) {
            writeJson(response, 401, "未登录或登录已过期，请重新登录");
            return false;
        }
        request.setAttribute(SESSION_ATTR, session);
        return true;
    }

    static void writeJson(HttpServletResponse response, int status, String message) throws Exception {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        byte[] bytes = ("{\"code\":" + status + ",\"msg\":\"" + message + "\",\"data\":null}")
                .getBytes(StandardCharsets.UTF_8);
        response.getOutputStream().write(bytes);
    }
}
