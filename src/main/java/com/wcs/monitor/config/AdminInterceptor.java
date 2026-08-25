package com.wcs.monitor.config;

import com.wcs.monitor.common.TokenStore;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AdminInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        Object attr = request.getAttribute(AuthInterceptor.SESSION_ATTR);
        if (attr instanceof TokenStore.Session session && !"admin".equals(session.role())) {
            AuthInterceptor.writeJson(response, 403, "无权限，该操作仅管理员可用");
            return false;
        }
        return true;
    }
}
