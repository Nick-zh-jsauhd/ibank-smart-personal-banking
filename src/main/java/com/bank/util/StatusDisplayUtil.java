package com.bank.util;

public final class StatusDisplayUtil {
    private StatusDisplayUtil() {
    }

    public static String simulationRunStatus(String status) {
        if ("RUNNING".equals(status)) return "运行中";
        if ("COMPLETED".equals(status)) return "已完成";
        if ("COMPLETED_WITH_WARNINGS".equals(status)) return "完成但有预警";
        if ("STOPPED".equals(status)) return "已停止";
        if ("FAILED".equals(status)) return "失败";
        if ("BLOCKED".equals(status)) return "已阻断";
        if (status == null || status.trim().length() == 0) return "暂无批次";
        return status;
    }

    public static String simulationEventStatus(String status) {
        if ("SUCCESS".equals(status)) return "成功";
        if ("RECORDED".equals(status)) return "已记录";
        if ("WARNING".equals(status)) return "预警";
        if ("FAILED".equals(status)) return "失败";
        if (status == null || status.trim().length() == 0) return "未知";
        return status;
    }
}
