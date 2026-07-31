package com.lingyi.ai.modules.smartreport.service;

import com.lingyi.ai.modules.smartreport.model.SmartReportResultVO;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SmartReportResultParserTest {

    private final SmartReportResultParser parser = new SmartReportResultParser();

    @Test
    void splitsAndMapsConclusionAndOperationSections() {
        String response = """
                ## 诊断结论
                🔴 **需立即关注：** 销售额大幅下滑（降幅 20.00%），大量链接下跌（占比 35.00%）
                🟡 **值得关注：** 部分链接未出单（占比 10.00%）
                ## 运营诊断报告
                ① 整体表现：今日销售额下降。
                ② 链接结构分析：未出单链接增加。
                ③ 异常逻辑判断：存在整体性下跌。
                ④ 运营建议：优先排查流量。
                """;

        SmartReportResultVO result = parser.parse(response);

        assertThat(result.getDiagnosisConclusions().getRedAlerts())
                .containsExactly("销售额大幅下滑（降幅 20.00%）", "大量链接下跌（占比 35.00%）");
        assertThat(result.getDiagnosisConclusions().getYellowAlerts())
                .containsExactly("部分链接未出单（占比 10.00%）");
        assertThat(result.getDiagnosisConclusions().getGreenHighlights()).isEmpty();
        assertThat(result.getOperationDiagnosis().getOverallPerformance()).isEqualTo("今日销售额下降。");
        assertThat(result.getOperationDiagnosis().getLinkStructureAnalysis()).isEqualTo("未出单链接增加。");
        assertThat(result.getOperationDiagnosis().getAnomalyLogicJudgment()).isEqualTo("存在整体性下跌。");
        assertThat(result.getOperationDiagnosis().getOperationSuggestions()).isEqualTo("优先排查流量。");
    }

    @Test
    void keepsConclusionWhenOperationSectionIsMissing() {
        SmartReportResultVO result = parser.parse("🔴 **需立即关注：** 销售额大幅下滑（降幅 20.00%）");

        assertThat(result.getDiagnosisConclusions().getRedAlerts())
                .containsExactly("销售额大幅下滑（降幅 20.00%）");
        assertThat(result.getOperationDiagnosis().getOverallPerformance()).isEmpty();
        assertThat(result.getOperationDiagnosisText()).isEmpty();
    }

    @Test
    void returnsEmptyStructuredFieldsWhenResponseHasNoRecognizedSections() {
        SmartReportResultVO result = parser.parse("## 运营诊断报告\n没有编号内容");

        assertThat(result.getDiagnosisConclusions().getRedAlerts()).isEmpty();
        assertThat(result.getDiagnosisConclusions().getYellowAlerts()).isEmpty();
        assertThat(result.getDiagnosisConclusions().getGreenHighlights()).isEmpty();
        assertThat(result.getOperationDiagnosis().getOverallPerformance()).isEmpty();
    }

    @Test
    void acceptsASectionWithoutNextMarkerAndLineBasedLabel() {
        SmartReportResultVO result = parser.parse("""
                ## 诊断结论
                🟢 **本周运营状态良好！**，上涨链接占比亮眼（占比 80.00%）
                🔴 **需立即关注：**
                ## 运营诊断报告
                ①整体表现
                正文内容
                """);

        assertThat(result.getDiagnosisConclusions().getGreenHighlights())
                .containsExactly("上涨链接占比亮眼（占比 80.00%）");
        assertThat(result.getDiagnosisConclusions().getRedAlerts()).isEmpty();
        assertThat(result.getOperationDiagnosis().getOverallPerformance()).isEqualTo("正文内容");
    }
}
