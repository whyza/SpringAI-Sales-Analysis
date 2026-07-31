package com.lingyi.ai.modules.smartreport.service;

import com.lingyi.ai.common.ai.AiAnalysisService;
import com.lingyi.ai.modules.smartreport.model.SmartReportProgress;
import com.lingyi.ai.modules.smartreport.model.SmartReportRequestDTO;
import com.lingyi.ai.modules.smartreport.model.SmartReportResultVO;
import com.lingyi.ai.modules.smartreport.model.SmartRuleConfigDO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

/**
 * 智能报告引擎服务实现。
 *
 * <p>负责协调阈值加载、AI 调用、结果持久化和进度通知，具体文本能力由内部组件承担。</p>
 */
@Slf4j
@Service
public class SmartReportEngineServiceImpl implements SmartReportEngineService {

    @Resource
    private AiAnalysisService aiAnalysisService;

    @Resource
    private SmartRuleConfigService smartRuleConfigService;

    @Override
    public SmartReportResultVO analyze(SmartReportRequestDTO request) {
        return analyze(request, SmartReportProgress.NOOP);
    }

    @Override
    public SmartReportResultVO analyze(SmartReportRequestDTO request, Consumer<String> progress) {
        loadGlobalThresholds(request);
        request.applyDefaults();

        progress.accept(SmartReportProgress.AI_ANALYSIS);
        String fullResponse = generateFullReport(request);

        progress.accept(SmartReportProgress.PARSING);
        SmartReportResultVO result = SmartReportResultParser.parse(fullResponse);
        progress.accept(SmartReportProgress.COMPLETE);

        return result;
    }


    private void loadGlobalThresholds(SmartReportRequestDTO request) {
        SmartRuleConfigDO globalConfig = smartRuleConfigService.loadConfig();
        if (globalConfig == null) {
            return;
        }
        if (request.getR1Threshold() == null) {
            request.setR1Threshold(globalConfig.getR1Threshold());
        }
        if (request.getR2Threshold() == null) {
            request.setR2Threshold(globalConfig.getR2Threshold());
        }
        if (request.getR3Threshold() == null) {
            request.setR3Threshold(globalConfig.getR3Threshold());
        }
        if (request.getY1Threshold() == null) {
            request.setY1Threshold(globalConfig.getY1Threshold());
        }
        if (request.getG1Threshold() == null) {
            request.setG1Threshold(globalConfig.getG1Threshold());
        }
        if (request.getG2Threshold() == null) {
            request.setG2Threshold(globalConfig.getG2Threshold());
        }
    }

    private String generateFullReport(SmartReportRequestDTO request) {
        try {
            return aiAnalysisService.callAiAnalysis(
                    SmartReportPromptBuilder.buildSystemPrompt(),
                    SmartReportPromptBuilder.buildUserPrompt(request)
            );
        } catch (Exception e) {
            log.warn("AI 生成完整报告失败，使用本地规则降级", e);
            return SmartReportFallbackGenerator.generate(request);
        }
    }
}
