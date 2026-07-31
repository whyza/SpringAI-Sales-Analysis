package com.lingyi.ai.modules.smartreport.service;

import com.lingyi.ai.modules.smartreport.model.SmartRuleConfigDO;

/**
 * 全局规则配置服务
 *
 * @author lingyi
 */
public interface SmartRuleConfigService {

    /**
     * 保存全局规则阈值（覆盖写入，仅保留一条）
     */
    void saveConfig(SmartRuleConfigDO config);

    /**
     * 加载全局规则阈值
     */
    SmartRuleConfigDO loadConfig();
}
