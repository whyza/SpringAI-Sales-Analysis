package com.lingyi.ai.service.smart.impl;

import com.lingyi.ai.model.vo.SmartReportResultVO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 智能报告 AI 文本解析器。
 *
 * <p>保持原有标题、编号和 emoji 解析规则，负责把模型文本转换为响应对象。</p>
 */
public class SmartReportResultParser {

    private static final String MARKER_OP_DIAGNOSIS = "## 运营诊断报告";

    public static SmartReportResultVO parse(String fullResponse) {
        AiResponseParts parts = splitAiResponse(fullResponse);
        StructuredDiagnosis parsed = parseStructuredDiagnosis(
                parts.operationDiagnosisText,
                parts.diagnosisConclusionText
        );

        SmartReportResultVO result = new SmartReportResultVO();
        result.setDiagnosisConclusionText(parts.diagnosisConclusionText);
        result.setOperationDiagnosisText(parts.operationDiagnosisText);

        SmartReportResultVO.DiagnosisConclusionVO dc = new SmartReportResultVO.DiagnosisConclusionVO();
        dc.setRedAlerts(parsed.redAlerts);
        dc.setYellowAlerts(parsed.yellowAlerts);
        dc.setGreenHighlights(parsed.greenHighlights);
        result.setDiagnosisConclusions(dc);

        SmartReportResultVO.OperationDiagnosisVO od = new SmartReportResultVO.OperationDiagnosisVO();
        od.setOverallPerformance(parsed.overallPerformance);
        od.setLinkStructureAnalysis(parsed.linkStructureAnalysis);
        od.setAnomalyLogicJudgment(parsed.anomalyLogicJudgment);
        od.setOperationSuggestions(parsed.operationSuggestions);
        result.setOperationDiagnosis(od);

        return result;
    }

    private static AiResponseParts splitAiResponse(String fullResponse) {
        int idx = fullResponse.indexOf(MARKER_OP_DIAGNOSIS);
        if (idx >= 0) {
            return new AiResponseParts(
                    fullResponse.substring(0, idx).replace("## 诊断结论", "").trim(),
                    fullResponse.substring(idx + MARKER_OP_DIAGNOSIS.length()).trim()
            );
        }
        return new AiResponseParts(fullResponse.trim(), "");
    }

    private static StructuredDiagnosis parseStructuredDiagnosis(String operationDiagnosisText,
                                                                String diagnosisConclusionText) {
        StructuredDiagnosis result = new StructuredDiagnosis();
        result.redAlerts = extractDiagnosisConclusionLines(diagnosisConclusionText, "🔴");
        result.yellowAlerts = extractDiagnosisConclusionLines(diagnosisConclusionText, "🟡");
        result.greenHighlights = extractDiagnosisConclusionLines(diagnosisConclusionText, "🟢");
        result.overallPerformance = extractSectionText(operationDiagnosisText, "①", "②");
        result.linkStructureAnalysis = extractSectionText(operationDiagnosisText, "②", "③");
        result.anomalyLogicJudgment = extractSectionText(operationDiagnosisText, "③", "④");
        result.operationSuggestions = extractSectionText(operationDiagnosisText, "④", null);
        return result;
    }

    private static String extractSectionText(String content, String currentNum, String nextNum) {
        if (content == null || content.trim().isEmpty()) {
            return "";
        }
        int fromIdx = content.indexOf(currentNum);
        if (fromIdx < 0) {
            return "";
        }
        int toIdx = (nextNum == null) ? content.length() : content.indexOf(nextNum, fromIdx + 1);
        if (toIdx < 0) {
            toIdx = content.length();
        }
        String section = content.substring(fromIdx + 1, toIdx).trim();
        int colonIdx = section.indexOf("：");
        if (colonIdx > 0 && colonIdx < 30) {
            section = section.substring(colonIdx + 1).trim();
        } else {
            int nl = section.indexOf("\n");
            if (nl > 0 && nl < 30) {
                section = section.substring(nl + 1).trim();
            }
        }
        return section;
    }

    private static List<String> extractDiagnosisConclusionLines(String diagnosisConclusion, String tag) {
        if (diagnosisConclusion == null || diagnosisConclusion.trim().isEmpty()) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        for (String line : diagnosisConclusion.split("\\r?\\n")) {
            if (!line.startsWith(tag)) {
                continue;
            }
            String cleaned = line
                    .replace(tag, "")
                    .replace("**", "")
                    .replace("需立即关注：", "")
                    .replace("值得关注：", "")
                    .replace("本周运营状态良好！", "")
                    .trim();
            if (cleaned.isEmpty()) {
                continue;
            }
            for (String item : cleaned.split("[，,]")) {
                String value = item.trim();
                if (!value.isEmpty()) {
                    result.add(value);
                }
            }
        }
        return result;
    }

    private record AiResponseParts(String diagnosisConclusionText, String operationDiagnosisText) {
    }

    private static class StructuredDiagnosis {
        private List<String> redAlerts;
        private List<String> yellowAlerts;
        private List<String> greenHighlights;
        private String overallPerformance;
        private String linkStructureAnalysis;
        private String anomalyLogicJudgment;
        private String operationSuggestions;
    }
}
