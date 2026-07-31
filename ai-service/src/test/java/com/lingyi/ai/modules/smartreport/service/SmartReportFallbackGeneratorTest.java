package com.lingyi.ai.modules.smartreport.service;

import com.lingyi.ai.modules.smartreport.model.SmartReportRequestDTO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class SmartReportFallbackGeneratorTest {

    private final SmartReportFallbackGenerator generator = new SmartReportFallbackGenerator();

    @Test
    void generatesSameAlertLevelsAndFourDiagnosisSections() {
        SmartReportRequestDTO request = request(
                new BigDecimal("8500"), new BigDecimal("10200"),
                320, 8, 22, 30, 15, 25);

        String report = generator.generate(request);

        assertThat(report)
                .startsWith("## 诊断结论\n🔴 **需立即关注：**")
                .contains("大量链接下跌（占比 36.67%）")
                .contains("大量链接未出单（占比 50.00%）")
                .contains("## 运营诊断报告")
                .contains("① 整体表现：")
                .contains("② 链接结构分析：")
                .contains("③ 异常逻辑判断：")
                .contains("④ 运营建议：");
    }

    @Test
    void usesStableConclusionWhenNoRuleIsTriggered() {
        SmartReportRequestDTO request = request(
                new BigDecimal("100"), new BigDecimal("100"),
                10, 0, 0, 0, 0, 0);

        String report = generator.generate(request);

        assertThat(report).startsWith("## 诊断结论\n📋 本日数据平稳，暂无明显异常");
        assertThat(report).contains("暂未发现明显异常，链接结构相对稳定。");
    }

    @Test
    void treatsPositiveRevenueAsFullGrowthWhenYesterdayRevenueIsZero() {
        SmartReportRequestDTO request = request(
                new BigDecimal("100"), BigDecimal.ZERO,
                1, 0, 0, 0, 0, 0);

        assertThat(generator.generate(request))
                .contains("销售额稳步增长（增幅 100.00%）");
    }

    @Test
    void treatsMissingBusinessMetricsAsZero() {
        SmartReportRequestDTO request = new SmartReportRequestDTO();
        request.applyDefaults();

        assertThat(generator.generate(request))
                .contains("今日销售额 0.00 元，昨日销售额 0.00 元，变化 ↑0.00%")
                .contains("暂未发现明显异常，链接结构相对稳定。");
    }

    @Test
    void keepsIndependentRedYellowAndGreenRules() {
        SmartReportRequestDTO request = request(
                new BigDecimal("70"), new BigDecimal("100"),
                1, 5, 1, 0, 5, 0);

        assertThat(generator.generate(request))
                .contains("销售额大幅下滑（降幅 30.00%）")
                .contains("部分链接下跌（占比 16.67%）")
                .contains("上涨链接占比亮眼（占比 83.33%）");
    }

    @Test
    void treatsNullLinkCountsAsZeroWhenOtherLinksExist() {
        SmartReportRequestDTO request = new SmartReportRequestDTO();
        request.setTodayRisingLinks(10);
        request.applyDefaults();

        assertThat(generator.generate(request))
                .contains("上涨链接占比亮眼（占比 100.00%）");
    }

    private SmartReportRequestDTO request(BigDecimal todayRevenue,
                                          BigDecimal yesterdayRevenue,
                                          int todayOrders,
                                          int todayRisingLinks,
                                          int todayFallingLinks,
                                          int todayNoOrderLinks,
                                          int yesterdayRisingLinks,
                                          int yesterdayNoOrderLinks) {
        SmartReportRequestDTO request = new SmartReportRequestDTO();
        request.setTodayRevenue(todayRevenue);
        request.setYesterdayRevenue(yesterdayRevenue);
        request.setTodayOrders(todayOrders);
        request.setYesterdayOrders(0);
        request.setTodayRisingLinks(todayRisingLinks);
        request.setYesterdayRisingLinks(yesterdayRisingLinks);
        request.setTodayFallingLinks(todayFallingLinks);
        request.setYesterdayFallingLinks(0);
        request.setTodayNoOrderLinks(todayNoOrderLinks);
        request.setYesterdayNoOrderLinks(yesterdayNoOrderLinks);
        request.applyDefaults();
        return request;
    }
}
