package com.lingyi.ai.modules.smartreport.controller;

import com.lingyi.ai.common.result.Result;
import com.lingyi.ai.modules.smartreport.model.SmartReportRequestDTO;
import com.lingyi.ai.modules.smartreport.model.SmartReportResultVO;
import com.lingyi.ai.modules.smartreport.service.SmartReportEngineService;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

/**
 * 通用结果封装
 *
 * @author lingyi
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SmartReportController {

    @Resource
    private SmartReportEngineService smartReportEngineService;

    /**
     * 智能报告分析（规则引擎 + AI 运营建议）
     * <p>
     * 未传业务数据时自动从数据库加载最新配置
     * </p>
     *
     * @param request 包含销售指标和规则阈值的请求（可选）
     * @return 触发规则、诊断摘要和 AI 建议
     */
    @PostMapping("/smart-report/analyze")
    public Result<SmartReportResultVO> analyzeSmartReport(
            @RequestBody(required = false) SmartReportRequestDTO request) {
        if (request == null) {
            request = new SmartReportRequestDTO();
        }
        log.info("收到智能报告分析请求，日期：{}，是否有业务数据：{}", request.getReportDate(), !request.isDataMissing());
        try {
            SmartReportResultVO result = smartReportEngineService.analyze(request);
            return Result.success(result);
        } catch (Exception e) {
            log.error("智能报告分析失败", e);
            return Result.error("智能报告分析失败：" + e.getMessage());
        }
    }

    /**
     * 智能报告分析（流式进度），通过 SSE 实时推送进度
     */
    @PostMapping("/smart-report/analyze-stream")
    public SseEmitter analyzeSmartReportStream(@RequestBody(required = false) SmartReportRequestDTO request) {
        SmartReportRequestDTO req = request != null ? request : new SmartReportRequestDTO();
        log.info("收到智能报告流式分析请求，日期：{}", req.getReportDate());
        SseEmitter emitter = new SseEmitter(120_000L);

        Thread.ofVirtual().start(() -> {
            try {
                SmartReportResultVO result = smartReportEngineService.analyze(
                        req, step -> {
                            try {
                                emitter.send(SseEmitter.event().name("progress").data(step));
                            } catch (IOException e) {
                                // client disconnected
                            }
                        });
                emitter.send(SseEmitter.event().name("result").data(result));
                emitter.complete();
            } catch (Exception e) {
                log.error("流式分析失败", e);
                emitter.completeWithError(e);
            }
        });
        return emitter;
    }


}
