package com.mukewang.ai.llmcost.autoconfigure.provider;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class QwenCostProvider implements LlmCostProvider {

    @Override
    public boolean supports(String modelName) {
        return modelName != null && (modelName.toLowerCase().startsWith("qwen") || modelName.toLowerCase().startsWith("qwq"));
    }

    @Override
    public String providerName() {
        return "Qwen";
    }

    @Override
    public CostResult calculate(String modelName, long inputTokens, long outputTokens, CostContext context) {
        // 阶梯判断基于单次请求总输入 token
        long effectiveInput = (context != null && context.getInputTokenTotal() != null)
                ? context.getInputTokenTotal() : inputTokens;

        BigDecimal inputPerMillion;
        BigDecimal outputPerMillion;
        String tier;

        if (modelName.contains("3.7-plus") || modelName.contains("3.5-plus")) {
            if (effectiveInput <= 256_000) {
                inputPerMillion = new BigDecimal("2");
                outputPerMillion = new BigDecimal("8");
                tier = "≤256K";
            } else {
                inputPerMillion = new BigDecimal("6");
                outputPerMillion = new BigDecimal("24");
                tier = "256K~1M";
            }
        } else if (modelName.contains("3.8-flash") || modelName.contains("3.7-flash")) {
            // Flash 系列简单单价（按 ≤32K 档位示例）
            inputPerMillion = new BigDecimal("0.2");
            outputPerMillion = new BigDecimal("0.8");
            tier = "Flash档";
        } else {
            // 默认兜底，实际项目建议抛异常或从配置读取
            inputPerMillion = new BigDecimal("2");
            outputPerMillion = new BigDecimal("8");
            tier = "默认";
        }

        BigDecimal inputCost = inputPerMillion
                .multiply(BigDecimal.valueOf(inputTokens))
                .divide(BigDecimal.valueOf(1_000_000), 8, RoundingMode.HALF_UP);

        BigDecimal outputCost = outputPerMillion
                .multiply(BigDecimal.valueOf(outputTokens))
                .divide(BigDecimal.valueOf(1_000_000), 8, RoundingMode.HALF_UP);

        return new CostResult(inputCost, outputCost, tier);
    }
}