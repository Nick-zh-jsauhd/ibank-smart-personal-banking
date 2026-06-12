package com.bank.service.impl;

import com.bank.bean.Account;
import com.bank.dao.AccountDao;
import com.bank.dao.BillDao;
import com.bank.dao.impl.AccountDaoImpl;
import com.bank.dao.impl.BillDaoImpl;
import com.bank.dto.BillReportQuery;
import com.bank.dto.BillReportSummary;
import com.bank.dto.BillReportView;
import com.bank.dto.CategorySummary;
import com.bank.dto.LedgerEntryView;
import com.bank.dto.ServiceResult;
import com.bank.dto.TimeBucketSummary;
import com.bank.service.BillReportService;
import com.bank.util.DBUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BillReportServiceImpl implements BillReportService {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final int TOP_MOVEMENT_LIMIT = 6;
    private static final int MAX_DETAIL_LIMIT = 10000;

    private final BillDao billDao = new BillDaoImpl();
    private final AccountDao accountDao = new AccountDaoImpl();

    @Override
    public ServiceResult<BillReportView> getReport(BillReportQuery query) {
        if (query == null) {
            return ServiceResult.failure("报表查询条件不能为空。");
        }
        String periodType = normalizePeriodType(query.getPeriodType());
        query.setPeriodType(periodType);
        query.setDirection(normalizeDirection(query.getDirection()));
        query.setTxnType(normalizeTxnType(query.getTxnType()));
        query.setDetailLimit(normalizeLimit(query.getDetailLimit()));

        PeriodRange range;
        try {
            range = resolveRange(query, periodType);
        } catch (IllegalArgumentException e) {
            return ServiceResult.failure(e.getMessage());
        }

        try (Connection connection = DBUtil.getConnection()) {
            String accountScope = resolveAccountScope(connection, query.getCustomerId(), query.getAccountId());
            List<CategorySummary> categories = billDao.findCategorySummaryByRange(
                    connection, query.getCustomerId(), query.getAccountId(), range.startAt, range.endAt,
                    query.getDirection(), query.getTxnType());
            List<TimeBucketSummary> rawBuckets = billDao.findTimeBucketSummary(
                    connection, query.getCustomerId(), query.getAccountId(), range.startAt, range.endAt,
                    query.getDirection(), query.getTxnType(), periodType);
            List<LedgerEntryView> topMovements = billDao.findTopMovements(
                    connection, query.getCustomerId(), query.getAccountId(), range.startAt, range.endAt,
                    query.getDirection(), query.getTxnType(), TOP_MOVEMENT_LIMIT);
            List<LedgerEntryView> entries = billDao.findEntriesByRange(
                    connection, query.getCustomerId(), query.getAccountId(), range.startAt, range.endAt,
                    query.getDirection(), query.getTxnType(), query.getDetailLimit());

            BillReportView view = new BillReportView();
            view.setPeriodType(periodType);
            view.setPeriodValue(range.periodValue);
            view.setPeriodLabel(range.periodLabel);
            view.setStartAt(range.startAt);
            view.setEndAt(range.endAt);
            view.setAccountScope(accountScope);
            view.setCategories(categories);
            view.setTimeBuckets(fillBuckets(periodType, range, rawBuckets));
            view.setTopMovements(topMovements);
            view.setEntries(entries);
            view.setSummary(buildSummary(categories, topMovements));
            view.setInsightText(buildInsightText(periodType, view.getSummary()));
            view.setNextStepText(buildNextStepText(view.getSummary()));
            return ServiceResult.success("收支分析报表生成成功。", view);
        } catch (SecurityException e) {
            return ServiceResult.failure(e.getMessage());
        } catch (SQLException e) {
            e.printStackTrace();
            return ServiceResult.failure("收支分析报表生成失败，请检查数据库状态或稍后重试。");
        }
    }

    private String resolveAccountScope(Connection connection, long customerId, Long accountId) throws SQLException {
        if (accountId == null) {
            return "全部账户";
        }
        Account account = accountDao.findById(connection, accountId);
        if (account == null || account.getCustomerId() == null || account.getCustomerId().longValue() != customerId) {
            throw new SecurityException("账户不存在或不属于当前客户。");
        }
        return account.getAccountNo();
    }

    private BillReportSummary buildSummary(List<CategorySummary> categories, List<LedgerEntryView> topMovements) {
        BillReportSummary summary = new BillReportSummary();
        BigDecimal totalIncome = BigDecimal.ZERO.setScale(2);
        BigDecimal totalExpense = BigDecimal.ZERO.setScale(2);
        int incomeCount = 0;
        int expenseCount = 0;

        for (CategorySummary category : categories) {
            BigDecimal amount = money(category.getTotalAmount());
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
        summary.setNetIncome(totalIncome.subtract(totalExpense).setScale(2, RoundingMode.HALF_UP));
        summary.setIncomeCount(incomeCount);
        summary.setExpenseCount(expenseCount);
        summary.setTotalCount(incomeCount + expenseCount);

        BigDecimal largestIncome = BigDecimal.ZERO.setScale(2);
        BigDecimal largestExpense = BigDecimal.ZERO.setScale(2);
        for (LedgerEntryView entry : topMovements) {
            BigDecimal amount = money(entry.getAmount());
            if ("IN".equals(entry.getDirection()) && amount.compareTo(largestIncome) > 0) {
                largestIncome = amount;
            } else if ("OUT".equals(entry.getDirection()) && amount.compareTo(largestExpense) > 0) {
                largestExpense = amount;
            }
        }
        summary.setLargestIncome(largestIncome);
        summary.setLargestExpense(largestExpense);
        if (totalIncome.compareTo(BigDecimal.ZERO) > 0) {
            summary.setSavingRate(summary.getNetIncome().multiply(new BigDecimal("100"))
                    .divide(totalIncome, 2, RoundingMode.HALF_UP));
        }
        return summary;
    }

    private List<TimeBucketSummary> fillBuckets(String periodType, PeriodRange range,
                                                List<TimeBucketSummary> rawBuckets) {
        Map<String, TimeBucketSummary> bucketMap = new HashMap<String, TimeBucketSummary>();
        for (TimeBucketSummary bucket : rawBuckets) {
            bucketMap.put(bucket.getBucketKey(), bucket);
        }

        List<TimeBucketSummary> filled = new ArrayList<TimeBucketSummary>();
        if (BillReportQuery.PERIOD_DAY.equals(periodType)) {
            for (int hour = 0; hour < 24; hour++) {
                String key = hour < 10 ? "0" + hour : String.valueOf(hour);
                filled.add(bucketOrEmpty(bucketMap, key, key + ":00"));
            }
        } else if (BillReportQuery.PERIOD_YEAR.equals(periodType)) {
            int year = range.year;
            for (int month = 1; month <= 12; month++) {
                YearMonth yearMonth = YearMonth.of(year, month);
                String key = yearMonth.format(YEAR_MONTH_FORMATTER);
                filled.add(bucketOrEmpty(bucketMap, key, month + "月"));
            }
        } else {
            YearMonth yearMonth = range.yearMonth;
            for (int day = 1; day <= yearMonth.lengthOfMonth(); day++) {
                LocalDate date = yearMonth.atDay(day);
                filled.add(bucketOrEmpty(bucketMap, date.toString(), day + "日"));
            }
        }
        return filled;
    }

    private TimeBucketSummary bucketOrEmpty(Map<String, TimeBucketSummary> bucketMap, String key, String label) {
        TimeBucketSummary existing = bucketMap.get(key);
        if (existing != null) {
            existing.setBucketLabel(label);
            existing.setTotalIncome(money(existing.getTotalIncome()));
            existing.setTotalExpense(money(existing.getTotalExpense()));
            existing.setNetIncome(existing.getTotalIncome().subtract(existing.getTotalExpense())
                    .setScale(2, RoundingMode.HALF_UP));
            return existing;
        }
        TimeBucketSummary empty = new TimeBucketSummary();
        empty.setBucketKey(key);
        empty.setBucketLabel(label);
        empty.setTotalIncome(BigDecimal.ZERO.setScale(2));
        empty.setTotalExpense(BigDecimal.ZERO.setScale(2));
        empty.setNetIncome(BigDecimal.ZERO.setScale(2));
        empty.setEntryCount(0);
        return empty;
    }

    private PeriodRange resolveRange(BillReportQuery query, String periodType) {
        LocalDateTime start;
        LocalDateTime end;
        PeriodRange range = new PeriodRange();
        if (BillReportQuery.PERIOD_DAY.equals(periodType)) {
            LocalDate date = parseDate(query.getDate());
            query.setDate(date.format(DATE_FORMATTER));
            start = date.atStartOfDay();
            end = date.plusDays(1).atStartOfDay();
            range.periodValue = query.getDate();
            range.periodLabel = query.getDate() + " 日账单";
        } else if (BillReportQuery.PERIOD_YEAR.equals(periodType)) {
            int year = parseYear(query.getYear());
            query.setYear(String.valueOf(year));
            start = LocalDate.of(year, 1, 1).atStartOfDay();
            end = LocalDate.of(year + 1, 1, 1).atStartOfDay();
            range.periodValue = query.getYear();
            range.periodLabel = query.getYear() + " 年账单";
            range.year = year;
        } else {
            YearMonth yearMonth = parseYearMonth(query.getYearMonth());
            query.setYearMonth(yearMonth.format(YEAR_MONTH_FORMATTER));
            start = yearMonth.atDay(1).atStartOfDay();
            end = yearMonth.plusMonths(1).atDay(1).atStartOfDay();
            range.periodValue = query.getYearMonth();
            range.periodLabel = query.getYearMonth() + " 月账单";
            range.yearMonth = yearMonth;
        }
        range.startAt = Timestamp.valueOf(start);
        range.endAt = Timestamp.valueOf(end);
        if (range.yearMonth == null) {
            range.yearMonth = YearMonth.from(start);
        }
        if (range.year == 0) {
            range.year = start.getYear();
        }
        return range;
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.trim().length() == 0) {
            return LocalDate.now();
        }
        try {
            return LocalDate.parse(value.trim(), DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("日期格式应为 YYYY-MM-DD。");
        }
    }

    private YearMonth parseYearMonth(String value) {
        if (value == null || value.trim().length() == 0) {
            return YearMonth.now();
        }
        try {
            return YearMonth.parse(value.trim(), YEAR_MONTH_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("月份格式应为 YYYY-MM。");
        }
    }

    private int parseYear(String value) {
        if (value == null || value.trim().length() == 0) {
            return LocalDate.now().getYear();
        }
        try {
            int year = Integer.parseInt(value.trim());
            if (year < 2000 || year > 2100) {
                throw new IllegalArgumentException("年份应在 2000 到 2100 之间。");
            }
            return year;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("年份格式应为 YYYY。");
        }
    }

    private String normalizePeriodType(String periodType) {
        if (BillReportQuery.PERIOD_DAY.equals(periodType) || BillReportQuery.PERIOD_YEAR.equals(periodType)) {
            return periodType;
        }
        return BillReportQuery.PERIOD_MONTH;
    }

    private String normalizeDirection(String direction) {
        if ("IN".equals(direction) || "OUT".equals(direction)) {
            return direction;
        }
        return null;
    }

    private String normalizeTxnType(String txnType) {
        if (txnType == null || txnType.trim().length() == 0) {
            return null;
        }
        String normalized = txnType.trim().toUpperCase();
        return normalized.matches("[A-Z0-9_]{1,30}") ? normalized : null;
    }

    private int normalizeLimit(int limit) {
        if (limit <= 0) {
            return 500;
        }
        return Math.min(limit, MAX_DETAIL_LIMIT);
    }

    private BigDecimal money(BigDecimal amount) {
        if (amount == null) {
            return BigDecimal.ZERO.setScale(2);
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private String buildInsightText(String periodType, BillReportSummary summary) {
        if (summary.getTotalCount() == 0) {
            return "这个周期还没有形成账务记录，完成转账、缴费、存取款或理财交易后会自动汇总。";
        }
        String name = BillReportQuery.PERIOD_DAY.equals(periodType) ? "今天"
                : (BillReportQuery.PERIOD_YEAR.equals(periodType) ? "这一年" : "这个月");
        if (summary.getNetIncome().compareTo(BigDecimal.ZERO) >= 0) {
            return name + "收入覆盖了支出，净流入为正，可以继续关注资金是否适合转入理财或保留备用金。";
        }
        return name + "支出高于收入，建议优先查看最大支出和分类排行，确认是否存在可延后或异常的交易。";
    }

    private String buildNextStepText(BillReportSummary summary) {
        if (summary.getTotalCount() == 0) {
            return "下一步可以先完成一笔转账或生活缴费，系统会把交易同步到流水和账单报表。";
        }
        if (summary.getLargestExpense().compareTo(new BigDecimal("10000.00")) >= 0) {
            return "下一步建议核对最大支出交易，如果并非本人操作，可以进入安全中心或服务工单处理。";
        }
        return "下一步可以导出明细用于对账，或打印当前报表作为本周期资金复盘记录。";
    }

    private static class PeriodRange {
        private Timestamp startAt;
        private Timestamp endAt;
        private String periodValue;
        private String periodLabel;
        private YearMonth yearMonth;
        private int year;
    }
}
