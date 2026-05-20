package com.lingyi.ai.service.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * 销售消息发送客户端
 *
 * @author lingyi
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SalesMessageClient {

    private final RestClient.Builder restClientBuilder;

    /**
     * 发送分析结果消息
     *
     * @param date    日期 yyyy-MM-dd
     * @param message 消息正文
     */
    public void sendMessage(String date, String message) {
        RestClient client = restClientBuilder
                .baseUrl("http://192.168.110.80:8080")
                .build();
        log.info("发送销售消息，date={}", date);
        try {
            client.post()
                    .uri("/api/sales/sendMsg?date={date}", date)
                    .header("Content-Type", "application/json")
                    .body(message)
                    .retrieve()
                    .toBodilessEntity();
            log.info("销售消息发送成功，date={}", date);
        } catch (Exception e) {
            log.error("发送销售消息失败，date={}", date, e);
        }
    }
}
