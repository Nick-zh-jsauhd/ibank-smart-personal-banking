package com.bank.dto;

public class WealthSettlementSummary {
    private int pendingBuyOrders;
    private int redeemingOrders;
    private int confirmedBuyOrders;
    private int settledRedeemOrders;

    public int getPendingBuyOrders() {
        return pendingBuyOrders;
    }

    public void setPendingBuyOrders(int pendingBuyOrders) {
        this.pendingBuyOrders = pendingBuyOrders;
    }

    public int getRedeemingOrders() {
        return redeemingOrders;
    }

    public void setRedeemingOrders(int redeemingOrders) {
        this.redeemingOrders = redeemingOrders;
    }

    public int getConfirmedBuyOrders() {
        return confirmedBuyOrders;
    }

    public void setConfirmedBuyOrders(int confirmedBuyOrders) {
        this.confirmedBuyOrders = confirmedBuyOrders;
    }

    public int getSettledRedeemOrders() {
        return settledRedeemOrders;
    }

    public void setSettledRedeemOrders(int settledRedeemOrders) {
        this.settledRedeemOrders = settledRedeemOrders;
    }
}
