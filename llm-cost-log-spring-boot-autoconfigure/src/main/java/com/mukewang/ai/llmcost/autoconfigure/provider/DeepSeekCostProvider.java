package com.mukewang.ai.llmcost.autoconfigure.provider;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class DeepSeekCostProvider implements LlmCostProvider {

    @Override
    public boolean supports(String modelName) {
        return modelName != null && modelName.toLowerCase().startsWith("deepseek");
    }

    @Override
    public String providerName() {
        return "DeepSeek";
    }

    @Override
    public CostResult calculate(String modelName, long inputTokens, long outputTokens, CostContext context) {
        boolean peak = context != null && context.isPeakHour();
        boolean cacheHit = context != null && context.isCacheHit();

        // 按百万 token 单价（元），依据搜索结果设置默认值，可改为从配置读取
        BigDecimal inputPerMillion;
        BigDecimal outputPerMillion;

        if (modelName.contains("v4-pro")) {
            if (cacheHit) {
                inputPerMillion = peak ? new BigDecimal("0.30") : new BigDecimal("0.15");
            } else {
                inputPerMillion = peak ? new BigDecimal("9.0") : new BigDecimal("4.5");
            }
            outputPerMillion = peak ? new BigDecimal("27.0") : new BigDecimal("13.5");
        } else {
            // deepseek-flash 默认
            if (cacheHit) {
                inputPerMillion = peak ? new BigDecimal("0.04") : new BigDecimal("0.02");
            } else {
                inputPerMillion = peak ? new BigDecimal("2") : new BigDecimal("1");
            }
            outputPerMillion = peak ? new BigDecimal("8") : new BigDecimal("4");
        }

        BigDecimal inputCost = inputPerMillion
                .multiply(BigDecimal.valueOf(inputTokens))
                .divide(BigDecimal.valueOf(1_000_000), 8, RoundingMode.HALF_UP);

        BigDecimal outputCost = outputPerMillion
                .multiply(BigDecimal.valueOf(outputTokens))
                .divide(BigDecimal.valueOf(1_000_000), 8, RoundingMode.HALF_UP);

        String detail = (peak ? "高峰" : "空闲") + (cacheHit ? "/缓存命中" : "/缓存未命中");
        return new CostResult(inputCost, outputCost, detail);
    }
}