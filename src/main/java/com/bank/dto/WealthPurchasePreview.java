package com.bank.dto;

import com.bank.bean.Account;
import com.bank.bean.WealthProduct;

import java.math.BigDecimal;

public class WealthPurchasePreview {
    private WealthProduct product;
    private Account account;
    private BigDecimal amount;
    private String customerRiskLevel;
    private String productRiskLevel;
    private String matchResult;
    private String disclosureVersion;

    public WealthProduct getProduct() {
        return product;
    }

    public void setProduct(WealthProduct product) {
        this.product = product;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCustomerRiskLevel() {
        return customerRiskLevel;
    }

    public void setCustomerRiskLevel(String customerRiskLevel) {
        this.customerRiskLevel = customerRiskLevel;
    }

    public String getProductRiskLevel() {
        return productRiskLevel;
    }

    public void setProductRiskLevel(String productRiskLevel) {
        this.productRiskLevel = productRiskLevel;
    }

    public String getMatchResult() {
        return matchResult;
    }

    public void setMatchResult(String matchResult) {
        this.matchResult = matchResult;
    }

    public String getDisclosureVersion() {
        return disclosureVersion;
    }

    public void setDisclosureVersion(String disclosureVersion) {
        this.disclosureVersion = disclosureVersion;
    }
}
