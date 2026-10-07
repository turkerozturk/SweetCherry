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
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        boolean protectedPost = "POST".equalsIgnoreCase(request.getMethod())
                && (request.getRequestURI().startsWith(request.getContextPath() + "/nodes/delete/")
                || request.getRequestURI().startsWith(request.getContextPath() + "/nodes/properties/")
                || request.getRequestURI().startsWith(request.getContextPath() + "/nodes/content/")
                || request.getRequestURI().startsWith(request.getContextPath() + "/nodes/richtext/edit/")
                || request.getRequestURI().startsWith(request.getContextPath() + "/nodes/move/")
                || request.getRequestURI().startsWith(request.getContextPath() + "/nodes/duplicate/")
                || request.getRequestURI().startsWith(request.getContextPath() + "/nodes/pdf/")
                || request.getRequestURI().startsWith(request.getContextPath() + "/bookmarks/")
                || request.getRequestURI().startsWith(request.getContextPath() + "/nodes/children/")
                || request.getRequestURI().startsWith(request.getContextPath() + "/nodes/siblings/")
                || request.getRequestURI().equals(request.getContextPath() + "/nodes/top-level")
                || request.getRequestURI().startsWith(request.getContextPath() + "/expo/")
                || request.getRequestURI().startsWith(request.getContextPath() + "/exportWithSubNodes/")
                || request.getRequestURI().equals(request.getContextPath() + "/mindmap-export"));
        String pageToken = request.getParameter("_tenantView");
        boolean markedNavigation = pageToken != null;
        if (!requiresTenant(handlerMethod) && !protectedPost && !markedNavigation) {
            return true;
        }
        if (!TenantContext.hasCurrentTenant()) {
            response.sendRedirect(request.getContextPath() + "/");
            return false;
        }
        if (protectedPost || markedNavigation) {
            jakarta.servlet.http.HttpSession session = request.getSession(false);
            String expected = session == null ? null : (String) session.getAttribute(
                    TenantContext.SESSION_VARIABLE__TENANT_VIEW_TOKEN);
            if (expected == null || !expected.equals(pageToken)) {
                response.sendError(HttpServletResponse.SC_CONFLICT,
                        "Veri kaynağı değişti. Sayfayı yeniden açıp işlemi tekrar deneyin.");
                return false;
            }
        }
        return true;
    }

    private boolean requiresTenant(HandlerMethod handlerMethod) {
        return AnnotatedElementUtils.hasAnnotation(handlerMethod.getMethod(), RequiresTenant.class)
                || AnnotatedElementUtils.hasAnnotation(handlerMethod.getBeanType(), RequiresTenant.class);
    }
}
