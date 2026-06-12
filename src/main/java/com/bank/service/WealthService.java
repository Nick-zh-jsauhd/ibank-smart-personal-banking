package com.bank.service;

import com.bank.bean.WealthProduct;
import com.bank.dto.ServiceResult;
import com.bank.dto.TransactionResult;
import com.bank.dto.WealthHoldingView;
import com.bank.dto.WealthOrderView;
import com.bank.dto.WealthPurchasePreview;
import com.bank.dto.WealthSettlementSummary;

import java.util.List;

public interface WealthService {
    ServiceResult<List<WealthProduct>> listProducts();

    ServiceResult<WealthProduct> getProduct(long productId);

    ServiceResult<WealthPurchasePreview> previewPurchase(long customerId, long productId,
                                                         long accountId, String amountText);

    ServiceResult<TransactionResult> buyProduct(long customerId, long userId, long productId,
                                                long accountId, String amountText, String payPassword,
                                                String ipAddress);

    ServiceResult<TransactionResult> confirmBuyProduct(long customerId, long userId, long productId,
                                                       long accountId, String amountText, String payPassword,
                                                       boolean productDisclosureChecked,
                                                       boolean nonDepositChecked,
                                                       boolean yieldNotGuaranteedChecked,
                                                       boolean accountConfirmed,
                                                       String ipAddress);

    ServiceResult<List<WealthHoldingView>> listHoldings(long customerId);

    ServiceResult<TransactionResult> redeemHolding(long customerId, long userId, long holdingId,
                                                   String payPassword, String ipAddress);

    ServiceResult<List<WealthOrderView>> listSettlementOrders(String status);

    ServiceResult<WealthSettlementSummary> settlementSummary();

    ServiceResult<WealthSettlementSummary> runSettlement(long adminUserId, String action, String ipAddress);
}
