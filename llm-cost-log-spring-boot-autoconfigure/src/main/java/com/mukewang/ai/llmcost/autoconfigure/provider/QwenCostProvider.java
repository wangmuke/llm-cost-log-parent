package com.mukewang.ai.llmcost.autoconfigure.provider;

import com.mukewang.ai.llmcost.autoconfigure.LlmPricingProperties;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class QwenCostProvider implements LlmCostProvider {

    private final LlmPricingProperties properties;

    public QwenCostProvider(LlmPricingProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean supports(String modelName) {
        return modelName != null
                && (modelName.toLowerCase().startsWith("qwen") || modelName.toLowerCase().startsWith("qwq"));
    }

    @Override
    public String providerName() {
        return "Qwen";
    }

    @Override
    public CostResult calculate(String modelName, long inputTokens, long outputTokens, CostContext context) {
        long effectiveInput = (context != null && context.getInputTokenTotal() != null)
                ? context.getInputTokenTotal() : inputTokens;

        BigDecimal inputPerMillion = null;
        BigDecimal outputPerMillion = null;
        String tier;

        LlmPricingProperties.ModelPrice cfg = properties.findPrice(modelName);
        if (cfg != null && cfg.getInputPricePerMillion() != null && cfg.getOutputPricePerMillion() != null) {
            inputPerMillion = cfg.getInputPricePerMillion();
            outputPerMillion = cfg.getOutputPricePerMillion();
            tier = "配置单价";
        } else {
            if (modelName.contains("3.7-plus") || modelName.contains("3.5-plus")) {
                if (effectiveInput <= 256_000) {
                    inputPerMillion = new BigDecimal("2");
                    outputPerMillion = new BigDecimal("8");
                    tier = "≤256K(内置)";
                } else {
                    inputPerMillion = new BigDecimal("6");
                    outputPerMillion = new BigDecimal("24");
                    tier = "256K~1M(内置)";
                }
            } else if (modelName.contains("3.8-flash") || modelName.contains("3.7-flash")) {
                inputPerMillion = new BigDecimal("0.2");
                outputPerMillion = new BigDecimal("0.8");
                tier = "Flash档(内置)";
            } else {
                inputPerMillion = new BigDecimal("2");
                outputPerMillion = new BigDecimal("8");
                tier = "默认(内置)";
            }
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