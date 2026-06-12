package com.bank.service;

import com.bank.dto.BillReportQuery;
import com.bank.dto.BillReportView;
import com.bank.dto.ServiceResult;

public interface BillReportService {
    ServiceResult<BillReportView> getReport(BillReportQuery query);
}
