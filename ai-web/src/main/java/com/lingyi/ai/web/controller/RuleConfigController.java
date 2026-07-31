package com.lingyi.ai.web.controller;

import com.lingyi.ai.dal.base.Result;
import com.lingyi.ai.dal.dataobject.SmartReportConfigDO;
import com.lingyi.ai.dal.dataobject.SmartRuleConfigDO;
import com.lingyi.ai.model.dto.SmartReportRequestDTO;
import com.lingyi.ai.service.smart.SmartReportConfigService;
import com.lingyi.ai.service.smart.SmartRuleConfigService;
import com.lingyi.ai.service.util.SalesMessageClient;
import com.lingyi.ai.service.util.SalesSummaryClient;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * 通用结果封装
 *
 * @author lingyi
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RuleConfigController {
    @Resource
    private SmartReportConfigService smartReportConfigService;

    @Resource
    private SmartRuleConfigService smartRuleConfigService;

    @Resource
    private SalesSummaryClient salesSummaryClient;

    @Resource
    private SalesMessageClient salesMessageClient;

    /**
     * 保存全局规则阈值
     */
    @PostMapping("/smart-report/rule-config/save")
    public Result<Void> saveSmartRuleConfig(@RequestBody SmartRuleConfigDO config) {
        if (config == null || config.getR1Threshold() == null) {
            return Result.error("请求体不能为空");
        }
        log.info("收到保存全局规则阈值请求");
        try {
            smartRuleConfigService.saveConfig(config);
            return Result.success(null);
        } catch (Exception e) {
            log.error("保存全局规则阈值失败", e);
            return Result.error("保存失败：" + e.getMessage());
        }
    }

    /**
     * 加载全局规则阈值
     */
    @GetMapping("/smart-report/rule-config/load")
    public Result<SmartRuleConfigDO> loadSmartRuleConfig() {
        log.info("收到加载全局规则阈值请求");
        try {
            SmartRuleConfigDO config = smartRuleConfigService.loadConfig();
            return Result.success(config);
        } catch (Exception e) {
            log.error("加载全局规则阈值失败", e);
            return Result.error("加载失败：" + e.getMessage());
        }
    }

    /**
     * 保存智能报告配置（业务数据）
     */
    @PostMapping("/smart-report/config/save")
    public Result<Void> saveSmartReportConfig(@RequestBody SmartReportRequestDTO request) {
        log.info("收到保存智能报告配置请求");
        try {
            smartReportConfigService.saveConfig(toConfigDO(request));
            return Result.success(null);
        } catch (Exception e) {
            log.error("保存智能报告配置失败", e);
            return Result.error("保存失败：" + e.getMessage());
        }
    }

    /**
     * 加载指定日期的智能报告业务数据（默认当天）
     */
    @GetMapping("/smart-report/config/load")
    public Result<SmartReportConfigDO> loadSmartReportConfig(
            @RequestParam(name = "date", required = false) String date) {
        log.info("收到加载智能报告配置请求，date={}", date);
        try {
            LocalDate loadDate = date != null ? LocalDate.parse(date) : LocalDate.now();
            SmartReportConfigDO config = smartReportConfigService.loadByDate(loadDate);
            return Result.success(config);
        } catch (Exception e) {
            log.error("加载智能报告配置失败", e);
            return Result.error("加载失败：" + e.getMessage());
        }
    }

    private SmartReportConfigDO toConfigDO(SmartReportRequestDTO request) {
        SmartReportConfigDO config = new SmartReportConfigDO();
        config.setReportDate(request.getReportDate());
        config.setTodayRevenue(request.getTodayRevenue());
        config.setYesterdayRevenue(request.getYesterdayRevenue());
        config.setTodayOrders(request.getTodayOrders());
        config.setYesterdayOrders(request.getYesterdayOrders());
        config.setTodayRisingLinks(request.getTodayRisingLinks());
        config.setYesterdayRisingLinks(request.getYesterdayRisingLinks());
        config.setTodayFallingLinks(request.getTodayFallingLinks());
        config.setYesterdayFallingLinks(request.getYesterdayFallingLinks());
        config.setTodayNoOrderLinks(request.getTodayNoOrderLinks());
        config.setYesterdayNoOrderLinks(request.getYesterdayNoOrderLinks());
        config.setR1Threshold(request.getR1Threshold());
        config.setR2Threshold(request.getR2Threshold());
        config.setR3Threshold(request.getR3Threshold());
        config.setY1Threshold(request.getY1Threshold());
        config.setG1Threshold(request.getG1Threshold());
        config.setG2Threshold(request.getG2Threshold());
        return config;
    }

}
