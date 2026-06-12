package com.bank.servlet;

import com.bank.dto.BillReportQuery;
import com.bank.dto.BillReportSummary;
import com.bank.dto.BillReportView;
import com.bank.dto.CategorySummary;
import com.bank.dto.LedgerEntryView;
import com.bank.dto.ServiceResult;
import com.bank.dto.SessionUser;
import com.bank.service.BillReportService;
import com.bank.service.impl.BillReportServiceImpl;
import com.bank.util.RequestUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@WebServlet(name = "BillExportServlet", urlPatterns = "/bill/export")
public class BillExportServlet extends HttpServlet {
    private static final int EXPORT_DETAIL_LIMIT = 10000;
    private static final DateTimeFormatter EXPORT_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final BillReportService billReportService = new BillReportServiceImpl();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SessionUser sessionUser = (SessionUser) request.getSession().getAttribute("loginUser");
        BillReportQuery query = buildQuery(request, sessionUser);
        ServiceResult<BillReportView> result = billReportService.getReport(query);
        if (!result.isSuccess()) {
            response.setCharacterEncoding("UTF-8");
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write(result.getMessage());
            return;
        }

        BillReportView report = result.getData();
        String filename = "iBank-" + report.getPeriodType().toLowerCase() + "-report-"
                + LocalDateTime.now().format(EXPORT_TIME_FORMATTER) + ".csv";
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''"
                + URLEncoder.encode(filename, "UTF-8").replace("+", "%20"));

        try (PrintWriter writer = response.getWriter()) {
            writer.write('\uFEFF');
            writeReport(report, writer);
        }
    }

    private BillReportQuery buildQuery(HttpServletRequest request, SessionUser sessionUser) {
        BillReportQuery query = new BillReportQuery();
        query.setCustomerId(sessionUser.getCustomerId());
        query.setAccountId(parseAccountId(RequestUtil.trim(request, "accountId")));
        query.setPeriodType(RequestUtil.trim(request, "periodType"));
        query.setDate(RequestUtil.trim(request, "date"));
        query.setYearMonth(RequestUtil.trim(request, "yearMonth"));
        query.setYear(RequestUtil.trim(request, "year"));
        query.setDirection(RequestUtil.trim(request, "direction"));
        query.setTxnType(RequestUtil.trim(request, "txnType"));
        query.setDetailLimit(EXPORT_DETAIL_LIMIT);
        return query;
    }

    private void writeReport(BillReportView report, PrintWriter writer) {
        BillReportSummary summary = report.getSummary();
        csvLine(writer, "iBank 收支分析报表");
        csvLine(writer, "报表周期", report.getPeriodLabel());
        csvLine(writer, "账户范围", report.getAccountScope());
        csvLine(writer, "生成时间", LocalDateTime.now().toString().replace('T', ' '));
        csvLine(writer, "");

        csvLine(writer, "摘要");
        csvLine(writer, "总收入", money(summary.getTotalIncome()));
        csvLine(writer, "总支出", money(summary.getTotalExpense()));
        csvLine(writer, "净流入", money(summary.getNetIncome()));
        csvLine(writer, "收入笔数", String.valueOf(summary.getIncomeCount()));
        csvLine(writer, "支出笔数", String.valueOf(summary.getExpenseCount()));
        csvLine(writer, "交易总笔数", String.valueOf(summary.getTotalCount()));
        csvLine(writer, "最大收入", money(summary.getLargestIncome()));
        csvLine(writer, "最大支出", money(summary.getLargestExpense()));
        csvLine(writer, "储蓄率", money(summary.getSavingRate()) + "%");
        csvLine(writer, "");

        csvLine(writer, "分类统计");
        csvLine(writer, "方向", "交易类型", "笔数", "金额");
        for (CategorySummary category : report.getCategories()) {
            csvLine(writer, directionName(category.getDirection()), txnTypeName(category.getTxnType()),
                    String.valueOf(category.getEntryCount()), money(category.getTotalAmount()));
        }
        csvLine(writer, "");

        csvLine(writer, "明细流水");
        csvLine(writer, "时间", "交易编号", "账户", "类型", "方向", "金额", "交易后余额", "状态", "摘要", "备注");
        for (LedgerEntryView entry : report.getEntries()) {
            csvLine(writer,
                    timeText(entry.getCreatedAt()),
                    entry.getTransactionNo(),
                    entry.getAccountNo(),
                    txnTypeName(entry.getTxnType()),
                    directionName(entry.getDirection()),
                    money(entry.getAmount()),
                    money(entry.getBalanceAfter()),
                    entry.getStatus(),
                    entry.getSummary(),
                    entry.getRemark());
        }
    }

    private void csvLine(PrintWriter writer, String... values) {
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                writer.print(',');
            }
            writer.print(csv(values[i]));
        }
        writer.println();
    }

    private String csv(String value) {
        String safe = value == null ? "" : value;
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }

    private String money(BigDecimal amount) {
        if (amount == null) {
            return "0.00";
        }
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String timeText(Timestamp timestamp) {
        return timestamp == null ? "" : timestamp.toString().substring(0, 19);
    }

    private String directionName(String direction) {
        if ("IN".equals(direction)) {
            return "收入";
        }
        if ("OUT".equals(direction)) {
            return "支出";
        }
        return direction == null ? "" : direction;
    }

    private String txnTypeName(String txnType) {
        if ("DEPOSIT".equals(txnType)) return "存款";
        if ("WITHDRAW".equals(txnType)) return "取款";
        if ("TRANSFER_INNER".equals(txnType)) return "本行转账";
        if ("PAYMENT".equals(txnType)) return "生活缴费";
        if ("BUY_WEALTH".equals(txnType)) return "理财申购";
        if ("REDEEM_WEALTH".equals(txnType)) return "理财赎回";
        return txnType == null ? "其他交易" : txnType;
    }

    private Long parseAccountId(String value) {
        if (value == null || value.length() == 0) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
