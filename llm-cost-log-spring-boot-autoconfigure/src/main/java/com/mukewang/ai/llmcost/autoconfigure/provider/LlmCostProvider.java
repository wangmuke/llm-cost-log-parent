package com.mukewang.ai.llmcost.autoconfigure.provider;

import java.math.BigDecimal;

public interface LlmCostProvider {

    /**
     * 判断当前 Provider 是否支持该模型
     */
    boolean supports(String modelName);

    /**
     * 计算一次调用的成本
     *
     * @param modelName   模型名
     * @param inputTokens 输入 token 数
     * @param outputTokens 输出 token 数
     * @param context     上下文（如时段、缓存命中、阶梯信息，可为 null）
     * @return 成本明细
     */
    CostResult calculate(String modelName, long inputTokens, long outputTokens, CostContext context);

    /**
     * 供应商标识，用于日志
     */
    String providerName();

    class CostResult {
        private final BigDecimal inputCost;
        private final BigDecimal outputCost;
        private final BigDecimal totalCost;
        private final String detail; // 计费说明，如 "高峰时段" "阶梯256K"

        public CostResult(BigDecimal inputCost, BigDecimal outputCost, String detail) {
            this.inputCost = inputCost;
            this.outputCost = outputCost;
            this.totalCost = inputCost.add(outputCost);
            this.detail = detail;
        }

        public BigDecimal getInputCost() {
            return inputCost;
        }

        public BigDecimal getOutputCost() {
            return outputCost;
        }

        public BigDecimal getTotalCost() {
            return totalCost;
        }

        public String getDetail() {
            return detail;
        }

    }

    class CostContext {
        private boolean peakHour = false;      // DeepSeek 用
        private boolean cacheHit = false;      // 缓存命中
        private Long inputTokenTotal;          // Qwen 阶梯用（若为 null 则用 inputTokens）

        public boolean isPeakHour() {
            return peakHour;
        }

        public void setPeakHour(boolean peakHour) {
            this.peakHour = peakHour;
        }

        public boolean isCacheHit() {
            return cacheHit;
        }

        public void setCacheHit(boolean cacheHit) {
            this.cacheHit = cacheHit;
        }

        public Long getInputTokenTotal() {
            return inputTokenTotal;
        }

        public void setInputTokenTotal(Long inputTokenTotal) {
            this.inputTokenTotal = inputTokenTotal;
        }
    }
}