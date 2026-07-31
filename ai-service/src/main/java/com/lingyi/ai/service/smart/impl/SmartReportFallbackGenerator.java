package com.lingyi.ai.service.smart.impl;

import com.lingyi.ai.model.dto.SmartReportRequestDTO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * 智能报告本地降级生成器。
 *
 * <p>保留原有规则判断和模板文本，用于 AI 调用失败时生成兼容结果。</p>
 */
public class SmartReportFallbackGenerator {

    private static final String DEFAULT_CONCLUSION = "📋 本日数据平稳，暂无明显异常";

    public static String generate(SmartReportRequestDTO req) {
        String fallbackDC = buildFallbackDiagnosisConclusion(req);
        String fallbackOD = buildFallbackOperationDiagnosis(req, fallbackDC);
        return "## 诊断结论\n" + fallbackDC + "\n\n## 运营诊断报告\n" + fallbackOD;
    }

    private static String buildFallbackDiagnosisConclusion(SmartReportRequestDTO req) {
        TriggeredRules triggeredRules = evaluateFallbackRules(req);
        List<String> lines = new ArrayList<>();
        if (!triggeredRules.getRedAlerts().isEmpty()) {
            lines.add("🔴 **需立即关注：** " + String.join("，", triggeredRules.getRedAlerts()));
        }
        if (!triggeredRules.getYellowAlerts().isEmpty()) {
            lines.add("🟡 **值得关注：** " + String.join("，", triggeredRules.getYellowAlerts()));
        }
        if (!triggeredRules.getGreenAlerts().isEmpty()) {
            lines.add("🟢 **本周运营状态良好！** " + String.join("，", triggeredRules.getGreenAlerts()));
        }
        if (lines.isEmpty()) {
            return DEFAULT_CONCLUSION;
        }
        return String.join("\n", lines);
    }

    private static String buildFallbackOperationDiagnosis(SmartReportRequestDTO req, String diagnosisConclusion) {
        int todayTotalLinks = calculateTodayTotalLinks(req);
        int yesterdayTotalLinks = calculateYesterdayTotalLinks(req);
        String revenueChange = formatPercentTrend(calculateRevenueChange(req));
        int noOrderDiff = safeInt(req.getTodayNoOrderLinks()) - safeInt(req.getYesterdayNoOrderLinks());

        StringBuilder sb = new StringBuilder();

        sb.append("① 整体表现：");
        sb.append("今日销售额 ").append(formatAmount(req.getTodayRevenue())).append(" 元，昨日销售额 ")
          .append(formatAmount(req.getYesterdayRevenue())).append(" 元，变化 ").append(revenueChange).append("。");
        sb.append("今日订单量 ").append(safeInt(req.getTodayOrders())).append(" 单，昨日订单量 ")
          .append(safeInt(req.getYesterdayOrders())).append(" 单。");
        sb.append("今日总链接数 ").append(todayTotalLinks).append("，昨日总链接数 ").append(yesterdayTotalLinks).append(
                "。\n\n");

        sb.append("② 链接结构分析：");
        sb.append("上涨 ").append(safeInt(req.getTodayRisingLinks())).append(" 个，下跌 ")
          .append(safeInt(req.getTodayFallingLinks())).append(" 个，未出单 ")
          .append(safeInt(req.getTodayNoOrderLinks())).append(" 个，总链接 ").append(todayTotalLinks).append(" 个。");
        sb.append("昨日未出单 ").append(safeInt(req.getYesterdayNoOrderLinks())).append(" 个，未出单变化 ")
          .append(noOrderDiff).append(" 个。\n\n");

        sb.append("③ 异常逻辑判断：");
        boolean hasRed = diagnosisConclusion.contains("🔴");
        boolean hasYellow = diagnosisConclusion.contains("🟡");
        if (hasRed || hasYellow) {
            sb.append("触发预警规则，需关注异常链接。");
            if (hasRed) {
                sb.append(" ⚠️ 存在红色预警，需立即排查。");
            }
            if (hasYellow) {
                sb.append(" ⚠️ 存在黄色预警，建议关注。");
            }
        } else {
            sb.append("暂未发现明显异常，链接结构相对稳定。");
        }
        sb.append("\n\n");

        sb.append("④ 运营建议：\n");
        sb.append("✅ 优先排查销售额变化原因，检查客单价是否稳定。弱势行情以排查为主，避免大幅改动。\n");
        sb.append("✅ 重点跟进下跌链接和未出单链接，及时优化 listing、广告投放和竞价策略，区分平台原因还是店铺原因。\n");
        sb.append("✅ 对已表现较好的上涨链接，继续承接流量，维持广告预算，避免因结构失衡拖累整体表现。\n");

        return sb.toString();
    }

    private static TriggeredRules evaluateFallbackRules(SmartReportRequestDTO req) {
        int todayTotalLinks = calculateTodayTotalLinks(req);
        BigDecimal revenueChange = calculateRevenueChange(req);

        TriggeredRules result = new TriggeredRules();
        result.setRedAlerts(new ArrayList<>());
        result.setYellowAlerts(new ArrayList<>());
        result.setGreenAlerts(new ArrayList<>());

        BigDecimal fallingRatio = BigDecimal.ZERO;
        BigDecimal noOrderRatio = BigDecimal.ZERO;
        BigDecimal risingRatio = BigDecimal.ZERO;
        if (todayTotalLinks > 0) {
            fallingRatio = calculateLinkRatio(req.getTodayFallingLinks(), todayTotalLinks);
            noOrderRatio = calculateLinkRatio(req.getTodayNoOrderLinks(), todayTotalLinks);
            risingRatio = calculateLinkRatio(req.getTodayRisingLinks(), todayTotalLinks);
        }

        if (revenueChange.compareTo(BigDecimal.ZERO) < 0 && revenueChange.abs().compareTo(req.getR1Threshold()) >= 0) {
            result.getRedAlerts().add("销售额大幅下滑（降幅 " + formatPercentFlat(revenueChange.abs()) + "）");
        }
        if (todayTotalLinks > 0) {
            if (fallingRatio.compareTo(req.getR2Threshold()) >= 0) {
                result.getRedAlerts().add("大量链接下跌（占比 " + formatPercentFlat(fallingRatio) + "）");
            }
            if (noOrderRatio.compareTo(req.getR3Threshold()) >= 0) {
                result.getRedAlerts().add("大量链接未出单（占比 " + formatPercentFlat(noOrderRatio) + "）");
            }
        }

        if (revenueChange.compareTo(BigDecimal.ZERO) < 0
                && revenueChange.abs().compareTo(req.getY1Threshold()) >= 0
                && revenueChange.abs().compareTo(req.getR1Threshold()) < 0) {
            result.getYellowAlerts().add("销售额小幅下滑（降幅 " + formatPercentFlat(revenueChange.abs()) + "）");
        }
        if (todayTotalLinks > 0) {
            if (safeInt(req.getTodayFallingLinks()) > 0 && fallingRatio.compareTo(req.getR2Threshold()) < 0) {
                result.getYellowAlerts().add("部分链接下跌（占比 " + formatPercentFlat(fallingRatio) + "）");
            }
            if (safeInt(req.getTodayNoOrderLinks()) > 0 && noOrderRatio.compareTo(req.getR3Threshold()) < 0) {
                result.getYellowAlerts().add("部分链接未出单（占比 " + formatPercentFlat(noOrderRatio) + "）");
            }
        }

        if (revenueChange.compareTo(req.getG1Threshold()) >= 0) {
            result.getGreenAlerts().add("销售额稳步增长（增幅 " + formatPercentFlat(revenueChange) + "）");
        }
        if (todayTotalLinks > 0 && risingRatio.compareTo(req.getG2Threshold()) >= 0) {
            result.getGreenAlerts().add("上涨链接占比亮眼（占比 " + formatPercentFlat(risingRatio) + "）");
        }

        return result;
    }

    private static BigDecimal calculateRevenueChange(SmartReportRequestDTO req) {
        BigDecimal yesterdayRevenue = defaultDecimal(req.getYesterdayRevenue());
        BigDecimal todayRevenue = defaultDecimal(req.getTodayRevenue());
        if (yesterdayRevenue.compareTo(BigDecimal.ZERO) <= 0) {
            return todayRevenue.compareTo(BigDecimal.ZERO) > 0 ? BigDecimal.valueOf(100) : BigDecimal.ZERO;
        }
        return todayRevenue.subtract(yesterdayRevenue).multiply(BigDecimal.valueOf(100))
                           .divide(yesterdayRevenue, 2, RoundingMode.HALF_UP);
    }

    private static int calculateTodayTotalLinks(SmartReportRequestDTO req) {
        return safeInt(req.getTodayRisingLinks()) + safeInt(req.getTodayFallingLinks()) + safeInt(req.getTodayNoOrderLinks());
    }

    private static int calculateYesterdayTotalLinks(SmartReportRequestDTO req) {
        return safeInt(req.getYesterdayRisingLinks()) + safeInt(req.getYesterdayFallingLinks()) + safeInt(req.getYesterdayNoOrderLinks());
    }

    private static BigDecimal calculateLinkRatio(Integer count, Integer totalLinks) {
        if (totalLinks == null || totalLinks <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(safeInt(count))
                         .multiply(BigDecimal.valueOf(100))
                         .divide(BigDecimal.valueOf(totalLinks), 2, RoundingMode.HALF_UP);
    }

    private static String formatPercentTrend(BigDecimal value) {
        BigDecimal safe = defaultDecimal(value);
        String prefix = safe.compareTo(BigDecimal.ZERO) >= 0 ? "↑" : "↓";
        return prefix + safe.abs().setScale(2, RoundingMode.HALF_UP).toPlainString() + "%";
    }

    private static String formatPercentFlat(BigDecimal value) {
        return defaultDecimal(value).setScale(2, RoundingMode.HALF_UP).toPlainString() + "%";
    }

    private static String formatAmount(BigDecimal value) {
        return defaultDecimal(value).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static BigDecimal defaultDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private static class TriggeredRules {
        private List<String> redAlerts;
        private List<String> yellowAlerts;
        private List<String> greenAlerts;

        private List<String> getRedAlerts() {
            return redAlerts;
        }

        private void setRedAlerts(List<String> redAlerts) {
            this.redAlerts = redAlerts;
        }

        private List<String> getYellowAlerts() {
            return yellowAlerts;
        }

        private void setYellowAlerts(List<String> yellowAlerts) {
            this.yellowAlerts = yellowAlerts;
        }

        private List<String> getGreenAlerts() {
            return greenAlerts;
        }

        private void setGreenAlerts(List<String> greenAlerts) {
            this.greenAlerts = greenAlerts;
        }
    }
}
