package com.bank.service;

import com.bank.dto.AdminRoleView;
import com.bank.dto.AdminUserDetail;
import com.bank.dto.AdminUserView;
import com.bank.dto.ServiceResult;

import java.util.List;

public interface AdminSecurityService {
    ServiceResult<List<AdminUserView>> listAdminUsers(String keyword, String status);

    ServiceResult<List<AdminRoleView>> listRoles();

    ServiceResult<AdminUserDetail> getAdminUserDetail(long userId);

    ServiceResult<Long> createAdmin(long operatorUserId, String username, String phone, String password,
                                    String[] roleCodes, String ipAddress);

    ServiceResult<Void> updateAdmin(long operatorUserId, long targetUserId, String status, String[] roleCodes,
                                    String resetPassword, String ipAddress);
}
