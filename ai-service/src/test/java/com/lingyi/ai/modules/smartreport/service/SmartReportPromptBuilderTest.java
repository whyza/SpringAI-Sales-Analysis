package com.lingyi.ai.modules.smartreport.service;

import com.lingyi.ai.modules.smartreport.model.SmartReportRequestDTO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class SmartReportPromptBuilderTest {

    private final SmartReportPromptBuilder promptBuilder = new SmartReportPromptBuilder();

    @Test
    void buildsPromptWithBusinessDataAndConfiguredThresholds() {
        SmartReportRequestDTO request = new SmartReportRequestDTO();
        request.setTodayRevenue(new BigDecimal("8500.00"));
        request.setYesterdayRevenue(new BigDecimal("10200.00"));
        request.setTodayOrders(320);
        request.setYesterdayOrders(410);
        request.setTodayRisingLinks(8);
        request.setYesterdayRisingLinks(15);
        request.setTodayFallingLinks(22);
        request.setYesterdayFallingLinks(10);
        request.setTodayNoOrderLinks(30);
        request.setYesterdayNoOrderLinks(25);
        request.setR1Threshold(new BigDecimal("20"));
        request.setR2Threshold(new BigDecimal("30"));
        request.setR3Threshold(new BigDecimal("30"));
        request.setY1Threshold(new BigDecimal("10"));
        request.setG1Threshold(new BigDecimal("10"));
        request.setG2Threshold(new BigDecimal("40"));

        String prompt = promptBuilder.buildUserPrompt(request);

        assertThat(prompt)
                .contains("昨日销售额：10200.00 元")
                .contains("今日销售额：8500.00 元")
                .contains("当天总链接数：60")
                .contains("销售额大幅下滑：降幅 = (昨日 - 当天) ÷ 昨日 × 100%，>= 20.00% 触发")
                .contains("上涨链接占比亮眼：上涨占比 = 当天上涨 ÷ 总链接 × 100%，>= 40.00% 触发");
    }

    @Test
    void keepsRequiredSectionsInSystemPrompt() {
        assertThat(promptBuilder.buildSystemPrompt())
                .contains("## 第一部分：诊断结论")
                .contains("## 第二部分：运营诊断报告")
                .contains("## 诊断结论")
                .contains("## 运营诊断报告");
    }

    @Test
    void formatsMissingMetricsAsZeroLikeTheOriginalEngine() {
        SmartReportRequestDTO request = new SmartReportRequestDTO();
        request.applyDefaults();

        assertThat(promptBuilder.buildUserPrompt(request))
                .contains("昨日销售额：0.00 元")
                .contains("今日订单量：0")
                .contains("当天总链接数：0");
    }
}
