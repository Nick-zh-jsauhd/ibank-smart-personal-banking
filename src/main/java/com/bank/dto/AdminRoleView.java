package com.bank.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AdminRoleView {
    private long roleId;
    private String roleCode;
    private String roleName;
    private String description;
    private String status;
    private final List<AdminPermissionView> permissions = new ArrayList<AdminPermissionView>();

    public long getRoleId() {
        return roleId;
    }

    public void setRoleId(long roleId) {
        this.roleId = roleId;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<AdminPermissionView> getPermissions() {
        return Collections.unmodifiableList(permissions);
    }

    public void addPermission(AdminPermissionView permission) {
        if (permission != null) {
            permissions.add(permission);
        }
    }
}
