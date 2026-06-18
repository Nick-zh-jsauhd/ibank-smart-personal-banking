package com.bank.util;

import java.util.Collection;

public final class AdminRoleDisplayUtil {
    private AdminRoleDisplayUtil() {
    }

    public static String roleName(String roleCode) {
        if ("SUPER_ADMIN".equals(roleCode)) return "超级管理员";
        if ("CUSTOMER_OPERATOR".equals(roleCode)) return "客户运营";
        if ("RISK_OPERATOR".equals(roleCode)) return "风控运营";
        if ("RISK_MANAGER".equals(roleCode)) return "风控规则管理员";
        if ("PRODUCT_MANAGER".equals(roleCode)) return "理财产品管理员";
        if ("ACCOUNTING_OPERATOR".equals(roleCode)) return "账务运营";
        if ("ACCOUNTING_REVIEWER".equals(roleCode)) return "账务复核员";
        if ("AUDITOR".equals(roleCode)) return "审计员";
        if (roleCode == null || roleCode.trim().length() == 0) return "未分配角色";
        return roleCode;
    }

    public static String primaryRoleName(Collection<String> roleCodes) {
        String primaryRole = primaryRoleCode(roleCodes);
        return roleName(primaryRole);
    }

    public static String roleSummary(Collection<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            return "未分配角色";
        }
        StringBuilder builder = new StringBuilder();
        for (String roleCode : roleCodes) {
            if (builder.length() > 0) {
                builder.append("、");
            }
            builder.append(roleName(roleCode));
        }
        return builder.toString();
    }

    public static String roleCountText(Collection<String> roleCodes) {
        int count = roleCodes == null ? 0 : roleCodes.size();
        if (count <= 0) return "权限待配置";
        if (count == 1) return "单一角色";
        return count + " 个角色";
    }

    public static String scopeText(Collection<String> roleCodes) {
        String primaryRole = primaryRoleCode(roleCodes);
        if ("SUPER_ADMIN".equals(primaryRole)) return "全域管理权限";
        if ("RISK_MANAGER".equals(primaryRole)) return "风控策略与模型复核";
        if ("RISK_OPERATOR".equals(primaryRole)) return "异常交易复核与处置";
        if ("ACCOUNTING_REVIEWER".equals(primaryRole)) return "调账与清算复核";
        if ("ACCOUNTING_OPERATOR".equals(primaryRole)) return "对账、异常与调账发起";
        if ("PRODUCT_MANAGER".equals(primaryRole)) return "理财产品与额度管理";
        if ("WEALTH_OPERATOR".equals(primaryRole)) return "理财运营处理";
        if ("CUSTOMER_OPERATOR".equals(primaryRole)) return "客户档案与服务跟进";
        if ("AUDITOR".equals(primaryRole)) return "审计查询与留痕核验";
        return "权限待配置";
    }

    private static String primaryRoleCode(Collection<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            return null;
        }
        if (roleCodes.contains("SUPER_ADMIN")) return "SUPER_ADMIN";
        if (roleCodes.contains("RISK_MANAGER")) return "RISK_MANAGER";
        if (roleCodes.contains("RISK_OPERATOR")) return "RISK_OPERATOR";
        if (roleCodes.contains("ACCOUNTING_REVIEWER")) return "ACCOUNTING_REVIEWER";
        if (roleCodes.contains("ACCOUNTING_OPERATOR")) return "ACCOUNTING_OPERATOR";
        if (roleCodes.contains("PRODUCT_MANAGER")) return "PRODUCT_MANAGER";
        if (roleCodes.contains("WEALTH_OPERATOR")) return "WEALTH_OPERATOR";
        if (roleCodes.contains("CUSTOMER_OPERATOR")) return "CUSTOMER_OPERATOR";
        if (roleCodes.contains("AUDITOR")) return "AUDITOR";
        return roleCodes.iterator().next();
    }
}
