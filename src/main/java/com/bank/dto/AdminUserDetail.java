package com.bank.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AdminUserDetail {
    private AdminUserView adminUser;
    private final List<AdminRoleView> availableRoles = new ArrayList<AdminRoleView>();

    public AdminUserView getAdminUser() {
        return adminUser;
    }

    public void setAdminUser(AdminUserView adminUser) {
        this.adminUser = adminUser;
    }

    public List<AdminRoleView> getAvailableRoles() {
        return Collections.unmodifiableList(availableRoles);
    }

    public void setAvailableRoles(List<AdminRoleView> roles) {
        availableRoles.clear();
        if (roles != null) {
            availableRoles.addAll(roles);
        }
    }
}
