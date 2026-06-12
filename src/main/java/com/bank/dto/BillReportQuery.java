package com.bank.dto;

public class BillReportQuery {
    public static final String PERIOD_DAY = "DAY";
    public static final String PERIOD_MONTH = "MONTH";
    public static final String PERIOD_YEAR = "YEAR";

    private long customerId;
    private Long accountId;
    private String periodType = PERIOD_MONTH;
    private String date;
    private String yearMonth;
    private String year;
    private String direction;
    private String txnType;
    private int detailLimit = 500;

    public long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(long customerId) {
        this.customerId = customerId;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public String getPeriodType() {
        return periodType;
    }

    public void setPeriodType(String periodType) {
        this.periodType = periodType;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getYearMonth() {
        return yearMonth;
    }

    public void setYearMonth(String yearMonth) {
        this.yearMonth = yearMonth;
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public String getTxnType() {
        return txnType;
    }

    public void setTxnType(String txnType) {
        this.txnType = txnType;
    }

    public int getDetailLimit() {
        return detailLimit;
    }

    public void setDetailLimit(int detailLimit) {
        this.detailLimit = detailLimit;
    }
}
