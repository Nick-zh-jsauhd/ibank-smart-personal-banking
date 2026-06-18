package com.bank.service.impl;

import com.bank.dao.AdminAuditLogDao;
import com.bank.dao.PermissionDao;
import com.bank.dao.impl.AdminAuditLogDaoImpl;
import com.bank.dao.impl.PermissionDaoImpl;
import com.bank.service.PermissionService;
import com.bank.util.DBUtil;

import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class PermissionServiceImpl implements PermissionService {
    private final PermissionDao permissionDao = new PermissionDaoImpl();
    private final AdminAuditLogDao adminAuditLogDao = new AdminAuditLogDaoImpl();

    @Override
    public Set<String> permissionsFor(long adminUserId) {
        try (Connection connection = DBUtil.getConnection()) {
            return permissionDao.findPermissionCodesByUserId(connection, adminUserId);
        } catch (SQLException e) {
            e.printStackTrace();
            return Collections.emptySet();
        }
    }

    @Override
    public Set<String> rolesFor(long adminUserId) {
        try (Connection connection = DBUtil.getConnection()) {
            return permissionDao.findRoleCodesByUserId(connection, adminUserId);
        } catch (SQLException e) {
            e.printStackTrace();
            return Collections.emptySet();
        }
    }

    @Override
    public String requiredPermission(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        String method = request.getMethod();
        if ("/admin/login".equals(path) || "/admin/logout".equals(path) || "/admin/forbidden.jsp".equals(path)) {
            return null;
        }
        if ("/admin/platform".equals(path) || "/admin/dashboard".equals(path) || "/admin/lab".equals(path)) {
            return "ADMIN_DASHBOARD_VIEW";
        }
        if ("/admin/simulation".equals(path) || "/admin/simulation/runtime".equals(path)) {
            return "ADMIN_DASHBOARD_VIEW";
        }
        if ("/admin/customers".equals(path) || "/admin/customer/detail".equals(path)) {
            return "CUSTOMER_VIEW";
        }
        if ("/admin/customer/risk-level".equals(path)) {
            return "CUSTOMER_RISK_ADJUST";
        }
        if ("/admin/risk/events".equals(path)) {
            return "RISK_EVENT_VIEW";
        }
        if ("/admin/risk/graph-models".equals(path)) {
            return "POST".equalsIgnoreCase(method) ? "RISK_GRAPH_CASE_HANDLE" : "RISK_GRAPH_SCORE_VIEW";
        }
        if ("/admin/risk/graph-scores".equals(path) || "/admin/risk/graph-neighborhood".equals(path)) {
            return "RISK_GRAPH_SCORE_VIEW";
        }
        if ("/admin/risk/graph-cases".equals(path)) {
            return "POST".equalsIgnoreCase(method) ? "RISK_GRAPH_CASE_HANDLE" : "RISK_GRAPH_CASE_VIEW";
        }
        if ("/admin/risk/graph-feedback-export".equals(path)) {
            return "RISK_GRAPH_CASE_HANDLE";
        }
        if ("/admin/risk/graph-case/detail".equals(path)) {
            return "POST".equalsIgnoreCase(method) ? "RISK_GRAPH_CASE_HANDLE" : "RISK_GRAPH_CASE_VIEW";
        }
        if ("/admin/risk/event/detail".equals(path)) {
            return "POST".equalsIgnoreCase(method) ? "RISK_EVENT_HANDLE" : "RISK_EVENT_VIEW";
        }
        if ("/admin/risk/rules".equals(path)) {
            return "POST".equalsIgnoreCase(method) ? "RISK_RULE_UPDATE" : "RISK_RULE_VIEW";
        }
        if ("/admin/wealth/products".equals(path)) {
            return "POST".equalsIgnoreCase(method) ? "WEALTH_PRODUCT_UPDATE" : "WEALTH_PRODUCT_VIEW";
        }
        if ("/admin/wealth/settlement".equals(path)) {
            return "POST".equalsIgnoreCase(method) ? "WEALTH_SETTLEMENT_RUN" : "WEALTH_SETTLEMENT_VIEW";
        }
        if ("/admin/messages".equals(path)) {
            return "ADMIN_ALERT_VIEW";
        }
        if ("/admin/message/detail".equals(path)) {
            return "POST".equalsIgnoreCase(method) ? "ADMIN_ALERT_HANDLE" : "ADMIN_ALERT_VIEW";
        }
        if ("/admin/tickets".equals(path)) {
            return "TICKET_VIEW";
        }
        if ("/admin/ticket/detail".equals(path)) {
            if (!"POST".equalsIgnoreCase(method)) {
                return "TICKET_VIEW";
            }
            String action = request.getParameter("action");
            if ("ASSIGN".equals(action)) {
                return "TICKET_ASSIGN";
            }
            return "TICKET_HANDLE";
        }
        if ("/admin/ticket/adjustment".equals(path)) {
            return "ADJUSTMENT_CREATE";
        }
        if ("/admin/reconciliation".equals(path)) {
            return "POST".equalsIgnoreCase(method) ? "RECONCILIATION_RUN" : "RECONCILIATION_VIEW";
        }
        if ("/admin/reconciliation/detail".equals(path)) {
            return "RECONCILIATION_VIEW";
        }
        if ("/admin/reconciliation/items".equals(path)) {
            return "RECONCILIATION_ITEM_VIEW";
        }
        if ("/admin/reconciliation/item/detail".equals(path)) {
            return "POST".equalsIgnoreCase(method) ? "RECONCILIATION_ITEM_HANDLE" : "RECONCILIATION_ITEM_VIEW";
        }
        if ("/admin/adjustments".equals(path)) {
            return "ADJUSTMENT_VIEW";
        }
        if ("/admin/adjustment/create".equals(path)) {
            return "ADJUSTMENT_CREATE";
        }
        if ("/admin/adjustment/detail".equals(path)) {
            if (!"POST".equalsIgnoreCase(method)) {
                return "ADJUSTMENT_VIEW";
            }
            String action = request.getParameter("action");
            if ("review".equals(action)) {
                return "ADJUSTMENT_REVIEW";
            }
            if ("execute".equals(action)) {
                return "ADJUSTMENT_EXECUTE";
            }
            return "ADJUSTMENT_VIEW";
        }
        if ("/admin/security/admins".equals(path)) {
            return "POST".equalsIgnoreCase(method) ? "ADMIN_USER_MANAGE" : "ADMIN_USER_VIEW";
        }
        if ("/admin/security/admin/detail".equals(path)) {
            return "POST".equalsIgnoreCase(method) ? "ADMIN_USER_MANAGE" : "ADMIN_USER_VIEW";
        }
        if ("/admin/security/roles".equals(path)) {
            return "ROLE_PERMISSION_VIEW";
        }
        if ("/admin/audit".equals(path) || "/admin/audit/admin-logs".equals(path)
                || "/admin/audit/login-logs".equals(path)) {
            return "ADMIN_AUDIT_VIEW";
        }
        return "ADMIN_DASHBOARD_VIEW";
    }

    @Override
    public boolean hasPermission(Set<String> permissions, String permissionCode) {
        if (permissionCode == null) {
            return true;
        }
        return permissions != null && permissions.contains(permissionCode);
    }

    @Override
    public void auditDenied(long adminUserId, String permissionCode, String path, String method, String ipAddress) {
        try (Connection connection = DBUtil.getConnection()) {
            adminAuditLogDao.insert(connection, adminUserId, "ADMIN_PERMISSION_DENIED", "PERMISSION",
                    permissionCode, "拒绝访问：" + method + " " + path, ipAddress);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
