package com.bank.dto;

import java.math.BigDecimal;

public class CategorySummary {
    private String txnType;
    private String direction;
    private BigDecimal totalAmount;
    private int entryCount;

    public String getTxnType() {
        return txnType;
    }

    public void setTxnType(String txnType) {
        this.txnType = txnType;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public int getEntryCount() {
        return entryCount;
    }

    public void setEntryCount(int entryCount) {
        this.entryCount = entryCount;
    }
}
