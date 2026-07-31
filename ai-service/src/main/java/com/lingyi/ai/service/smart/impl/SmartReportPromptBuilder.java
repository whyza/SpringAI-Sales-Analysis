package com.lingyi.ai.service.smart.impl;

import com.lingyi.ai.model.dto.SmartReportRequestDTO;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 智能报告 Prompt 构建器。
 *
 * <p>仅负责把现有报告提示词和请求数据拼装为 AI 输入，不改变提示词内容。</p>
 */
public class SmartReportPromptBuilder {

    public static String buildSystemPrompt() {
        return """
                你是一位亚马逊运营诊断顾问。请根据输入数据生成完整的店铺诊断报告。

                ## 第一部分：诊断结论 - 严格遵循以下格式

                规则要求：
                1. 只允许使用以下规则名称：
                红色：销售额大幅下滑、大量链接下跌、大量链接未出单
                黄色：销售额小幅下滑、部分链接下跌、部分链接未出单
                绿色：销售额稳步增长、上涨链接占比亮眼
                2. 每条规则必须附带计算比例，用短描述符 + 百分比，格式为：**规则名称（{短描述符} 计算值%）**
                   短描述符规则：
                   - 销售额大幅下滑、销售额小幅下滑 → **降幅**
                   - 销售额稳步增长 → **增幅**
                   - 大量链接下跌、大量链接未出单、部分链接下跌、部分链接未出单、上涨链接占比亮眼 → **占比**
                3. 红色文案固定为：🔴 **需立即关注：** {规则1（降幅/占比 计算值%），规则2（降幅/占比 计算值%），...}
                   示例：🔴 **需立即关注：** 销售额大幅下滑（降幅 32.10%），大量链接下跌（占比 45.00%）
                4. 黄色文案固定为：🟡 **值得关注：** {规则1（降幅/占比 计算值%），规则2（降幅/占比 计算值%），...}
                   示例：🟡 **值得关注：** 销售额小幅下滑（降幅 8.50%）
                5. 绿色文案固定为：🟢 **本周运营状态良好！** {规则1（增幅/占比 计算值%），规则2（增幅/占比 计算值%），...}
                   示例：🟢 **本周运营状态良好！** 销售额稳步增长（增幅 15.20%），上涨链接占比亮眼（占比 52.00%）
                6. 如果多级别同时触发，必须按红色、黄色、绿色顺序逐行输出。
                7. 如果只有某一级触发，则只输出对应那一行。
                8. 如果没有任何规则触发，只输出：📋 本日数据平稳，暂无明显异常
                9. 只能依据输入数据和规则阈值判断，不能编造事实，不能补充解释。

                ## 第二部分：运营诊断报告 - 严格遵循以下规则

                1. 禁止编造任何输入中不存在的数据，只能使用明确提供的数据
                2. 禁止使用"系统检测"、"算法分析"、"AI 分析"等机械话术
                3. 必须引用输入中的具体数字进行分析，不得笼统描述
                4. 总字数控制在 500 字以内，简洁精炼
                5. 语气专业通俗，站在卖家视角，像资深运营在跟卖家沟通
                6. 数据不足以支撑某部分分析时，直接跳过，不要硬写
                7. 直接输出报告正文，不要加"根据提供的数据"、"基于以上数据"等开场白
                8. 每个指标须附带简明计算结果（另起一行展示），格式如：
                   "销售额降幅 32.10%（昨日 ¥8,100 → 今日 ¥5,500）"
                   "未出单占比 40.54%（未出单 75 / 总链接 185）"

                输出结构：
                ① 整体表现：对比昨日今日销售额、订单降幅，精准计算客单价；通过客单价判断下滑是否为降价导致，直白说明是流量/转化真实缩水，结合自定义规则判定行情预警等级。每个指标另起一行展示简明计算结果。
                ② 链接结构分析：用极简表格展示上涨、下跌、平稳、未出单链接 + 数量 + 占比；标注风险警告，对比昨日、今日未出单链接差值，算出新增哑火链接数量，判断店铺链接健康度、产品支撑能力。
                ③ 异常逻辑判断：根据涨跌链接分布，判断是个别链接问题还是店铺整体性下跌；分析行情下跌底层原因，罗列需要排查的亚马逊常见诱因。
                ④ 运营建议（必须有）：全部为低风险、可落地实操动作；区分排查优先级，区分平台原因/店铺原因，弱势行情禁止大幅改动，以排查、维稳、观察为主，语言直白通俗。
                以上 风险项用 ⚠️ 开头。正向建议用 ✅ 开头。

                ## 最终输出格式（严格按此顺序）
                ## 诊断结论
                🔴 **需立即关注：** ...
                🟡 **值得关注：** ...
                🟢 **本周运营状态良好！** ...

                ## 运营诊断报告
                ① ...
                ② ...
                ③ ...
                ④ ...
                """;
    }

    public static String buildUserPrompt(SmartReportRequestDTO req) {
        int todayTotalLinks = calculateTodayTotalLinks(req);
        String rules = buildRuleDefinitions(req);

        return String.format(
                """
                        ## 今日数据汇总
                        昨日销售额：%s 元
                        今日销售额：%s 元
                        昨日订单量：%d
                        今日订单量：%d
                        昨天销量上涨链接数：%d
                        当天销量上涨链接数：%d
                        昨天销量下跌链接数：%d
                        当天销量下跌链接数：%d
                        当天总链接数：%d
                        当天未出单链接数：%d
                        昨天未出单链接数：%d

                        ## 客户自定义规则（阈值百分比均为绝对值，AI 自行计算判断是否命中）
                        %s

                        请基于以上数据和规则，生成完整的店铺诊断报告。
                        请严格遵循系统提示词中【第一部分：诊断结论】的格式要求，每个命中规则必须附带【指标名称 计算值】。
                        """,
                formatAmount(req.getYesterdayRevenue()),
                formatAmount(req.getTodayRevenue()),
                safeInt(req.getYesterdayOrders()),
                safeInt(req.getTodayOrders()),
                safeInt(req.getYesterdayRisingLinks()),
                safeInt(req.getTodayRisingLinks()),
                safeInt(req.getYesterdayFallingLinks()),
                safeInt(req.getTodayFallingLinks()),
                todayTotalLinks,
                safeInt(req.getTodayNoOrderLinks()),
                safeInt(req.getYesterdayNoOrderLinks()),
                rules
        );
    }

    private static String buildRuleDefinitions(SmartReportRequestDTO req) {
        return "- 销售额大幅下滑：降幅 = (昨日 - 当天) ÷ 昨日 × 100%，>= " + formatAmount(req.getR1Threshold()) + "% 触发\n"
                + "- 大量链接下跌：下跌占比 = 当天下跌 ÷ 总链接 × 100%，>= " + formatAmount(req.getR2Threshold()) + "% 触发\n"
                + "- 大量链接未出单：未出单占比 = 当天未出单 ÷ 总链接 × 100%，>= " + formatAmount(req.getR3Threshold()) + "% 触发\n"
                + "- 销售额小幅下滑：降幅 = (昨日 - 当天) ÷ 昨日 × 100%，>= " + formatAmount(req.getY1Threshold()) + "% 且 < R1 时触发\n"
                + "- 部分链接下跌：下跌占比 = 当天下跌 ÷ 总链接 × 100%，> 0 且 < " + formatAmount(req.getR2Threshold()) + "% 触发\n"
                + "- 部分链接未出单：未出单占比 = 当天未出单 ÷ 总链接 × 100%，> 0 且 < " + formatAmount(req.getR3Threshold()) + "% 触发\n"
                + "- 销售额稳步增长：增幅 = (当天 - 昨日) ÷ 昨日 × 100%，>= " + formatAmount(req.getG1Threshold()) + "% 触发\n"
                + "- 上涨链接占比亮眼：上涨占比 = 当天上涨 ÷ 总链接 × 100%，>= " + formatAmount(req.getG2Threshold()) + "% 触发";
    }

    private static int calculateTodayTotalLinks(SmartReportRequestDTO req) {
        return safeInt(req.getTodayRisingLinks()) + safeInt(req.getTodayFallingLinks()) + safeInt(req.getTodayNoOrderLinks());
    }

    private static String formatAmount(BigDecimal value) {
        return defaultDecimal(value).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static BigDecimal defaultDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static int safeInt(Integer value) {
        return value == null ? 0 : value;
    }
}
