package com.lingyi.ai.infrastructure.ai;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.lingyi.ai.common.ai.AiAnalysisService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * AI 分析服务实现（基于 Spring AI Alibaba）
 *
 * @author lingyi
 */
@Slf4j
@Service
public class AiAnalysisServiceImpl implements AiAnalysisService {

    private static final DashScopeChatOptions MULTI_MODEL_OPTIONS = DashScopeChatOptions.builder()
                                                                                        .withModel("qwen3.6-plus")
                                                                                        .withMultiModel(Boolean.TRUE)
                                                                                        .build();

    @Resource
    private ChatModel chatModel;


    @Override
    public String callAiAnalysis(String systemPrompt, String userPrompt) {
        log.info("调用 AI 分析，system 长度：{}, user 长度：{}", systemPrompt.length(), userPrompt.length());
        try {
            Prompt prompt = new Prompt(
                    List.of(new SystemMessage(systemPrompt), new UserMessage(userPrompt)),
                    MULTI_MODEL_OPTIONS
            );
            String result = chatModel.call(prompt).getResult().getOutput().getContent();
            log.info("AI 分析完成");
            return result;
        } catch (Exception e) {
            log.error("AI 调用失败", e);
            throw new RuntimeException("AI 分析失败，请稍后重试：" + e.getMessage(), e);
        }
    }
}
