package com.turkerozturk.multipledatabases;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class TenantRequiredInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod) || !requiresTenant(handlerMethod)) {
            return true;
        }

        if (TenantContext.hasCurrentTenant()) {
            return true;
        }

        response.sendRedirect(request.getContextPath() + "/");
        return false;
    }

    private boolean requiresTenant(HandlerMethod handlerMethod) {
        return AnnotatedElementUtils.hasAnnotation(handlerMethod.getMethod(), RequiresTenant.class)
                || AnnotatedElementUtils.hasAnnotation(handlerMethod.getBeanType(), RequiresTenant.class);
    }
}
