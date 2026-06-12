package com.bank.service.impl;

import com.bank.dao.BillDao;
import com.bank.dao.impl.BillDaoImpl;
import com.bank.dto.CategorySummary;
import com.bank.dto.LedgerEntryView;
import com.bank.dto.MonthlyBillSummary;
import com.bank.dto.ServiceResult;
import com.bank.service.BillService;
import com.bank.util.DBUtil;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

public class BillServiceImpl implements BillService {
    private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final int MONTHLY_ENTRY_LIMIT = 500;

    private final BillDao billDao = new BillDaoImpl();

    @Override
    public ServiceResult<MonthlyBillSummary> getMonthlyBill(long customerId, Long accountId, String yearMonthText) {
        YearMonth yearMonth;
        try {
            yearMonth = parseYearMonth(yearMonthText);
        } catch (IllegalArgumentException e) {
            return ServiceResult.failure(e.getMessage());
        }

        LocalDateTime start = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime end = yearMonth.plusMonths(1).atDay(1).atStartOfDay();

        try (Connection connection = DBUtil.getConnection()) {
            List<CategorySummary> categories = billDao.findCategorySummary(
                    connection,
                    customerId,
                    accountId,
                    Timestamp.valueOf(start),
                    Timestamp.valueOf(end)
            );
            List<LedgerEntryView> entries = billDao.findMonthlyEntries(
                    connection,
                    customerId,
                    accountId,
                    Timestamp.valueOf(start),
                    Timestamp.valueOf(end),
                    MONTHLY_ENTRY_LIMIT
            );
            MonthlyBillSummary summary = buildSummary(yearMonth, categories, entries);
            return ServiceResult.success("月账单查询成功。", summary);
        } catch (SQLException e) {
            e.printStackTrace();
            return ServiceResult.failure("月账单查询失败，请检查数据库状态或稍后重试。");
        }
    }

    private YearMonth parseYearMonth(String yearMonthText) {
        if (yearMonthText == null || yearMonthText.trim().length() == 0) {
            return YearMonth.now();
        }
        String normalized = yearMonthText.trim();
        if (!normalized.matches("\\d{4}-\\d{2}")) {
            throw new IllegalArgumentException("月份格式应为 YYYY-MM。");
        }
        try {
            return YearMonth.parse(normalized, YEAR_MONTH_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("月份格式不正确。");
        }
    }

    private MonthlyBillSummary buildSummary(YearMonth yearMonth, List<CategorySummary> categories,
                                            List<LedgerEntryView> entries) {
        MonthlyBillSummary summary = new MonthlyBillSummary();
        summary.setYearMonth(yearMonth.format(YEAR_MONTH_FORMATTER));
        summary.setCategories(categories);
        summary.setEntries(entries);

        BigDecimal totalIncome = BigDecimal.ZERO.setScale(2);
        BigDecimal totalExpense = BigDecimal.ZERO.setScale(2);
        int incomeCount = 0;
        int expenseCount = 0;
        for (CategorySummary category : categories) {
            BigDecimal amount = category.getTotalAmount() == null
                    ? BigDecimal.ZERO.setScale(2)
                    : category.getTotalAmount();
            if ("IN".equals(category.getDirection())) {
                totalIncome = totalIncome.add(amount);
                incomeCount += category.getEntryCount();
            } else if ("OUT".equals(category.getDirection())) {
                totalExpense = totalExpense.add(amount);
                expenseCount += category.getEntryCount();
            }
        }
        summary.setTotalIncome(totalIncome);
        summary.setTotalExpense(totalExpense);
        summary.setNetIncome(totalIncome.subtract(totalExpense));
        summary.setIncomeCount(incomeCount);
        summary.setExpenseCount(expenseCount);
        return summary;
    }
}
