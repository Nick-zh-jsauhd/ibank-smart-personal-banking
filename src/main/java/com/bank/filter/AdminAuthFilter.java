package com.bank.filter;

import com.bank.dto.AdminSessionUser;
import com.bank.service.PermissionService;
import com.bank.service.impl.PermissionServiceImpl;
import com.bank.util.RequestUtil;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebFilter(filterName = "AdminAuthFilter", urlPatterns = "/admin/*")
public class AdminAuthFilter implements Filter {
    private final PermissionService permissionService = new PermissionServiceImpl();

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String path = httpRequest.getRequestURI().substring(httpRequest.getContextPath().length());
        if ("/admin/login".equals(path)) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = httpRequest.getSession(false);
        if (session == null || session.getAttribute("adminUser") == null) {
            String next = URLEncoder.encode(httpRequest.getRequestURI(), "UTF-8");
            httpResponse.sendRedirect(httpRequest.getContextPath() + "/admin/login?next=" + next);
            return;
        }
        AdminSessionUser adminUser = (AdminSessionUser) session.getAttribute("adminUser");
        if (isDirectJspRequest(path)) {
            httpResponse.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String requiredPermission = permissionService.requiredPermission(httpRequest);
        if (requiredPermission != null && !adminUser.hasPermission(requiredPermission)) {
            Set<String> refreshedPermissions = permissionService.permissionsFor(adminUser.getUserId());
            adminUser.setPermissionCodes(refreshedPermissions);
            adminUser.setRoleCodes(permissionService.rolesFor(adminUser.getUserId()));
            if (!permissionService.hasPermission(refreshedPermissions, requiredPermission)) {
                permissionService.auditDenied(adminUser.getUserId(), requiredPermission, path,
                        httpRequest.getMethod(), RequestUtil.clientIp(httpRequest));
                httpResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
                httpRequest.setAttribute("requiredPermission", requiredPermission);
                httpRequest.setAttribute("requestPath", path);
                httpRequest.getRequestDispatcher("/admin/forbidden.jsp").forward(request, response);
                return;
            }
        }
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
    }

    private boolean isDirectJspRequest(String path) {
        return path != null && path.endsWith(".jsp") && !"/admin/forbidden.jsp".equals(path);
    }
}
