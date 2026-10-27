package com.mukewang.ai.llmcost.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "llm.cost")
public class LlmPricingProperties {

    /**
     * 模型单价映射：key 为模型名称（精确或前缀），value 为该模型的单价配置
     */
    private Map<String, ModelPrice> prices = new HashMap<>();

    public Map<String, ModelPrice> getPrices() {
        return prices;
    }

    public void setPrices(Map<String, ModelPrice> prices) {
        this.prices = prices;
    }

    /**
     * 按模型名查找单价配置：先精确匹配，再前缀匹配
     */
    public ModelPrice findPrice(String modelName) {
        if (modelName == null || prices.isEmpty()) {
            return null;
        }
        ModelPrice exact = prices.get(modelName);
        if (exact != null) {
            return exact;
        }
        // 前缀匹配，例如配置 "deepseek" 可覆盖所有 deepseek 模型
        return prices.entrySet().stream()
                .filter(e -> modelName.toLowerCase().startsWith(e.getKey().toLowerCase()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    public static class ModelPrice {
        /**
         * 每百万输入 Token 价格（元），单一单价场景使用
         */
        private BigDecimal inputPricePerMillion;
        /**
         * 每百万输出 Token 价格（元），单一单价场景使用
         */
        private BigDecimal outputPricePerMillion;

        /**
         * 高峰时段输入单价（元/百万），仅 DeepSeek 类场景使用，可选
         */
        private BigDecimal peakInputPricePerMillion;
        /**
         * 高峰时段输出单价（元/百万），可选
         */
        private BigDecimal peakOutputPricePerMillion;
        /**
         * 空闲时段输入单价（元/百万），可选
         */
        private BigDecimal idleInputPricePerMillion;
        /**
         * 空闲时段输出单价（元/百万），可选
         */
        private BigDecimal idleOutputPricePerMillion;
        /**
         * 缓存命中输入单价（元/百万），可选
         */
        private BigDecimal cacheHitInputPricePerMillion;

        // getters/setters 省略，实际代码需补全


        public BigDecimal getInputPricePerMillion() {
            return inputPricePerMillion;
        }

        public void setInputPricePerMillion(BigDecimal inputPricePerMillion) {
            this.inputPricePerMillion = inputPricePerMillion;
        }

        public BigDecimal getOutputPricePerMillion() {
            return outputPricePerMillion;
        }

        public void setOutputPricePerMillion(BigDecimal outputPricePerMillion) {
            this.outputPricePerMillion = outputPricePerMillion;
        }

        public BigDecimal getPeakInputPricePerMillion() {
            return peakInputPricePerMillion;
        }

        public void setPeakInputPricePerMillion(BigDecimal peakInputPricePerMillion) {
            this.peakInputPricePerMillion = peakInputPricePerMillion;
        }

        public BigDecimal getPeakOutputPricePerMillion() {
            return peakOutputPricePerMillion;
        }

        public void setPeakOutputPricePerMillion(BigDecimal peakOutputPricePerMillion) {
            this.peakOutputPricePerMillion = peakOutputPricePerMillion;
        }

        public BigDecimal getIdleInputPricePerMillion() {
            return idleInputPricePerMillion;
        }

        public void setIdleInputPricePerMillion(BigDecimal idleInputPricePerMillion) {
            this.idleInputPricePerMillion = idleInputPricePerMillion;
        }

        public BigDecimal getIdleOutputPricePerMillion() {
            return idleOutputPricePerMillion;
        }

        public void setIdleOutputPricePerMillion(BigDecimal idleOutputPricePerMillion) {
            this.idleOutputPricePerMillion = idleOutputPricePerMillion;
        }

        public BigDecimal getCacheHitInputPricePerMillion() {
            return cacheHitInputPricePerMillion;
        }

        public void setCacheHitInputPricePerMillion(BigDecimal cacheHitInputPricePerMillion) {
            this.cacheHitInputPricePerMillion = cacheHitInputPricePerMillion;
        }
    }
}