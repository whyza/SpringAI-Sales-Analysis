package com.lingyi.ai.service.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * 销售汇总数据查询客户端
 *
 * @author lingyi
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SalesSummaryClient {

    private final RestClient.Builder restClientBuilder;

    /**
     * 获取指定日期的销售汇总数据
     *
     * @param date 日期 yyyy-MM-dd
     * @return 销售汇总 JSON
     */
    public String fetchSummary(String date) {
        RestClient client = restClientBuilder
                .baseUrl("http://192.168.110.80:8080")
                .build();
        log.info("获取销售汇总数据，date={}", date);
        try {
            String result = client.get()
                    .uri("/api/sales/summary?date={date}", date)
                    .retrieve()
                    .body(String.class);
            log.info("销售汇总数据获取成功，date={}", date);
            return result;
        } catch (Exception e) {
            log.error("获取销售汇总数据失败，date={}", date, e);
            throw new RuntimeException("获取销售汇总数据失败: " + e.getMessage());
        }
    }
}
