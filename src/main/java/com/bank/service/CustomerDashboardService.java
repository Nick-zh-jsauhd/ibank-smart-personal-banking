package com.bank.service;

import com.bank.dto.CustomerDashboardView;
import com.bank.dto.ServiceResult;

public interface CustomerDashboardService {
    ServiceResult<CustomerDashboardView> loadDashboard(long customerId);
}
