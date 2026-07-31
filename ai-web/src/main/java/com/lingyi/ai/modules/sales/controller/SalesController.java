package com.lingyi.ai.modules.sales.controller;

import com.lingyi.ai.common.result.Result;
import com.lingyi.ai.infrastructure.sales.SalesMessageClient;
import com.lingyi.ai.infrastructure.sales.SalesSummaryClient;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 销售能力控制器。
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SalesController {

    @Resource
    private SalesSummaryClient salesSummaryClient;

    @Resource
    private SalesMessageClient salesMessageClient;

    /**
     * 代理销售汇总查询（调用外部服务）。
     */
    @GetMapping("/sales/summary-proxy")
    public Result<String> proxySalesSummary(@RequestParam("date") String date) {
        log.info("代理销售汇总查询，date={}", date);
        try {
            String data = salesSummaryClient.fetchSummary(date);
            return Result.success(data);
        } catch (Exception e) {
            log.error("代理销售汇总查询失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 发送销售消息。
     */
    @PostMapping("/sales/sendMsg")
    public Result<Void> sendSalesMessage(@RequestParam("date") String date, @RequestBody String message) {
        log.info("发送销售消息，date={}", date);
        try {
            salesMessageClient.sendMessage(date, message);
            return Result.success(null);
        } catch (Exception e) {
            log.error("发送销售消息失败", e);
            return Result.error(e.getMessage());
        }
    }
}
