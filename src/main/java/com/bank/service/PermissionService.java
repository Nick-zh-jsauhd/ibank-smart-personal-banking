package com.bank.service;

import javax.servlet.http.HttpServletRequest;
import java.util.Set;

public interface PermissionService {
    Set<String> permissionsFor(long adminUserId);

    Set<String> rolesFor(long adminUserId);

    String requiredPermission(HttpServletRequest request);

    boolean hasPermission(Set<String> permissions, String permissionCode);

    void auditDenied(long adminUserId, String permissionCode, String path, String method, String ipAddress);
}
