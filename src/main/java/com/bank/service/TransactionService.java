package com.bank.service;

import com.bank.dto.LedgerEntryView;
import com.bank.dto.ServiceResult;
import com.bank.dto.TransactionResult;
import com.bank.dto.BillPaymentView;

import java.util.List;

public interface TransactionService {
    ServiceResult<TransactionResult> deposit(long customerId, long userId, long accountId,
                                             String amountText, String remark, String ipAddress);

    ServiceResult<TransactionResult> withdraw(long customerId, long userId, long accountId,
                                              String amountText, String payPassword, String remark,
                                              String ipAddress);

    ServiceResult<TransactionResult> innerTransfer(long customerId, long userId, long fromAccountId,
                                                   String toAccountNo, String amountText, String payPassword,
                                                   String remark, String ipAddress);

    ServiceResult<TransactionResult> payBill(long customerId, long userId, long accountId,
                                             String paymentType, String payerNo, String billingMonth,
                                             String amountText, String payPassword, String remark,
                                             String ipAddress);

    ServiceResult<List<LedgerEntryView>> listLedgerEntries(long customerId, Long accountId,
                                                           String direction, String txnType);

    ServiceResult<List<BillPaymentView>> listBillPayments(long customerId, String paymentType);
}
