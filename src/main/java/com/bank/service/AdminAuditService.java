package com.bank.service;

import com.bank.dto.AdminAuditLogView;
import com.bank.dto.AdminAuditOverview;
import com.bank.dto.AdminLoginLogView;
import com.bank.dto.ServiceResult;

import java.util.List;

public interface AdminAuditService {
    ServiceResult<AdminAuditOverview> overview();

    ServiceResult<List<AdminAuditLogView>> listAdminAuditLogs(String adminKeyword, String operationType,
                                                              String targetType, String startDate,
                                                              String endDate, boolean highRiskOnly);

    ServiceResult<List<AdminLoginLogView>> listLoginLogs(String identityKeyword, String userRole,
                                                         String successFilter, String startDate,
                                                         String endDate);
}
