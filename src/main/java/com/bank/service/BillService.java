package com.bank.service;

import com.bank.dto.MonthlyBillSummary;
import com.bank.dto.ServiceResult;

public interface BillService {
    ServiceResult<MonthlyBillSummary> getMonthlyBill(long customerId, Long accountId, String yearMonthText);
}
