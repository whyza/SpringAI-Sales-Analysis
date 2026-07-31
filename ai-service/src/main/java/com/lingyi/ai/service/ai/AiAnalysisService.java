package com.lingyi.ai.service.ai;

/**
 * AI 分析服务接口
 *
 * @author lingyi
 */
public interface AiAnalysisService {

    /**
     * 调用 AI 进行电商数据分析（带系统提示词）
     *
     * @param systemPrompt 系统提示词
     * @param userPrompt   用户提示词
     * @return AI 分析结果
     */
    String callAiAnalysis(String systemPrompt, String userPrompt);

}
