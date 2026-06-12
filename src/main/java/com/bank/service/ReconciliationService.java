package com.bank.service;

import com.bank.bean.ReconciliationBatch;
import com.bank.bean.ReconciliationActionLog;
import com.bank.bean.ReconciliationItem;
import com.bank.dto.ServiceResult;

import java.time.LocalDate;
import java.util.List;

public interface ReconciliationService {
    ServiceResult<ReconciliationBatch> run(LocalDate reconDate, long adminUserId, String ipAddress);

    ServiceResult<List<ReconciliationBatch>> listRecentBatches();

    ServiceResult<ReconciliationBatch> getBatch(long batchId);

    ServiceResult<List<ReconciliationItem>> listItems(long batchId);

    ServiceResult<List<ReconciliationItem>> listExceptionItems(String status, String severity, String checkType);

    ServiceResult<ReconciliationItem> getItem(long itemId);

    ServiceResult<List<ReconciliationActionLog>> listActionLogs(long itemId);

    ServiceResult<Void> handleItem(long itemId, long adminUserId, String targetStatus, String note,
                                   String ipAddress);
}
