package com.bank.service;

import com.bank.bean.AdjustmentActionLog;
import com.bank.bean.AdjustmentRequest;
import com.bank.dto.ServiceResult;

import java.util.List;

public interface AdjustmentService {
    ServiceResult<AdjustmentRequest> createRequest(long itemId, long adminUserId, String accountNo,
                                                   String direction, String amountText, String reason,
                                                   String evidence, String ipAddress);

    ServiceResult<AdjustmentRequest> createFromTicket(long ticketId, long adminUserId, String accountNo,
                                                      String direction, String amountText, String reason,
                                                      String evidence, String ipAddress);

    ServiceResult<List<AdjustmentRequest>> listRequests(String status);

    ServiceResult<AdjustmentRequest> getRequest(long adjustmentId);

    ServiceResult<List<AdjustmentActionLog>> listActionLogs(long adjustmentId);

    ServiceResult<Void> review(long adjustmentId, long adminUserId, String decision, String note,
                               String ipAddress);

    ServiceResult<Void> execute(long adjustmentId, long adminUserId, String ipAddress);
}
