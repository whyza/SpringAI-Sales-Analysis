package com.lingyi.ai.modules.smartreport.controller;

import com.lingyi.ai.common.result.Result;
import com.lingyi.ai.modules.smartreport.model.SmartRuleConfigDO;
import com.lingyi.ai.modules.smartreport.service.SmartRuleConfigService;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 通用结果封装
 *
 * @author lingyi
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SmartRuleConfigController {

    @Resource
    private SmartRuleConfigService smartRuleConfigService;

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

}
