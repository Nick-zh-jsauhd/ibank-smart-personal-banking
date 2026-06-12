package com.bank.service;

import com.bank.bean.RiskLimitRule;
import com.bank.bean.WealthProduct;
import com.bank.dto.AdminCustomerDetail;
import com.bank.dto.AdminCustomerView;
import com.bank.dto.AdminDashboardMetrics;
import com.bank.dto.AdminRiskEventDetail;
import com.bank.dto.AdminRiskEventView;
import com.bank.dto.AdminSessionUser;
import com.bank.dto.ServiceResult;

import java.util.List;

public interface AdminService {
    ServiceResult<AdminSessionUser> login(String identity, String password, String ipAddress, String userAgent);

    ServiceResult<AdminDashboardMetrics> dashboardMetrics();

    ServiceResult<List<AdminCustomerView>> listCustomers(String keyword, String riskLevel);

    ServiceResult<AdminCustomerDetail> getCustomerDetail(long customerId, long adminUserId, String ipAddress);

    ServiceResult<Void> adjustCustomerRiskLevel(long adminUserId, long customerId, String riskLevel,
                                                String reason, String ipAddress);

    ServiceResult<List<AdminRiskEventView>> listRiskEvents(String decision, String handleStatus);

    ServiceResult<AdminRiskEventDetail> getRiskEventDetail(long eventId, long adminUserId, String ipAddress);

    ServiceResult<Void> handleRiskEvent(long adminUserId, long eventId, String handleResult,
                                        String accountAction, String note, String ipAddress);

    ServiceResult<List<RiskLimitRule>> listRiskRules(String txnType, String riskLevel);

    ServiceResult<Void> updateRiskRule(long adminUserId, long ruleId, String singleLimit,
                                       String dailyAmountLimit, String dailyCountLimit,
                                       String status, String ipAddress);

    ServiceResult<List<WealthProduct>> listWealthProducts(String status);

    ServiceResult<Void> updateWealthProduct(long adminUserId, long productId, String productName,
                                            String riskLevel, String expectedRate, String periodDays,
                                            String minAmount, String maxAmount, String status,
                                            String description, String ipAddress);

    void audit(long adminUserId, String operationType, String targetType, String targetId,
               String detail, String ipAddress);
}
