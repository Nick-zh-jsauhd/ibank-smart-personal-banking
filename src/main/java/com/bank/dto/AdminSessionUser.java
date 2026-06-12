package com.bank.dto;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class AdminSessionUser implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long userId;
    private String username;
    private String role;
    private Set<String> roleCodes = new HashSet<String>();
    private Set<String> permissionCodes = new HashSet<String>();

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Set<String> getRoleCodes() {
        return Collections.unmodifiableSet(roleCodes);
    }

    public void setRoleCodes(Set<String> roleCodes) {
        this.roleCodes = roleCodes == null ? new HashSet<String>() : new HashSet<String>(roleCodes);
    }

    public Set<String> getPermissionCodes() {
        return Collections.unmodifiableSet(permissionCodes);
    }

    public void setPermissionCodes(Set<String> permissionCodes) {
        this.permissionCodes = permissionCodes == null
                ? new HashSet<String>() : new HashSet<String>(permissionCodes);
    }

    public boolean hasPermission(String permissionCode) {
        return permissionCode == null || permissionCodes.contains(permissionCode);
    }

    public boolean hasRole(String roleCode) {
        return roleCode != null && roleCodes.contains(roleCode);
    }
}
