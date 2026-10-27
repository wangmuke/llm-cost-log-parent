package com.mukewang.ai.llmcost.autoconfigure.provider;

import com.mukewang.ai.llmcost.autoconfigure.LlmPricingProperties;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class DeepSeekCostProvider implements LlmCostProvider {

    private final LlmPricingProperties properties;

    public DeepSeekCostProvider(LlmPricingProperties properties) {
        this.properties = properties;
    }

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

        BigDecimal inputPerMillion = null;
        BigDecimal outputPerMillion = null;
        String detail = (peak ? "高峰" : "空闲") + (cacheHit ? "/缓存命中" : "/缓存未命中");

        // 1. 优先读外部配置
        LlmPricingProperties.ModelPrice cfg = properties.findPrice(modelName);
        if (cfg != null) {
            if (peak) {
                inputPerMillion = cfg.getPeakInputPricePerMillion();
                outputPerMillion = cfg.getPeakOutputPricePerMillion();
            } else {
                inputPerMillion = cfg.getIdleInputPricePerMillion();
                outputPerMillion = cfg.getIdleOutputPricePerMillion();
            }
            if (cacheHit && cfg.getCacheHitInputPricePerMillion() != null) {
                inputPerMillion = cfg.getCacheHitInputPricePerMillion();
            }
            // 单一单价兜底
            if (inputPerMillion == null && cfg.getInputPricePerMillion() != null) {
                inputPerMillion = cfg.getInputPricePerMillion();
            }
            if (outputPerMillion == null && cfg.getOutputPricePerMillion() != null) {
                outputPerMillion = cfg.getOutputPricePerMillion();
            }
        }

        // 2. 配置缺省时走内置默认
        if (inputPerMillion == null || outputPerMillion == null) {
            BigDecimal[] defaults = builtInPrice(modelName, peak, cacheHit);
            if (inputPerMillion == null) inputPerMillion = defaults[0];
            if (outputPerMillion == null) outputPerMillion = defaults[1];
            detail += "(内置)";
        } else {
            detail += "(配置)";
        }

        BigDecimal inputCost = inputPerMillion
                .multiply(BigDecimal.valueOf(inputTokens))
                .divide(BigDecimal.valueOf(1_000_000), 8, RoundingMode.HALF_UP);
        BigDecimal outputCost = outputPerMillion
                .multiply(BigDecimal.valueOf(outputTokens))
                .divide(BigDecimal.valueOf(1_000_000), 8, RoundingMode.HALF_UP);

        return new CostResult(inputCost, outputCost, detail);
    }

    private BigDecimal[] builtInPrice(String modelName, boolean peak, boolean cacheHit) {
        if (modelName.contains("v4-pro")) {
            BigDecimal input = cacheHit
                    ? (peak ? new BigDecimal("0.30") : new BigDecimal("0.15"))
                    : (peak ? new BigDecimal("9.0") : new BigDecimal("4.5"));
            BigDecimal output = peak ? new BigDecimal("27.0") : new BigDecimal("13.5");
            return new BigDecimal[]{input, output};
        }
        BigDecimal input = cacheHit
                ? (peak ? new BigDecimal("0.04") : new BigDecimal("0.02"))
                : (peak ? new BigDecimal("2") : new BigDecimal("1"));
        BigDecimal output = peak ? new BigDecimal("8") : new BigDecimal("4");
        return new BigDecimal[]{input, output};
    }
}