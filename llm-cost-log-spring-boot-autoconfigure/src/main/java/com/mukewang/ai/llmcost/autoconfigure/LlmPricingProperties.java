package com.mukewang.ai.llmcost.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "llm.cost")
public class LlmPricingProperties {

    /**
     * 模型单价映射：key 为模型名称，value 为该模型的输入/输出单价
     */
    private Map<String, ModelPrice> prices = new HashMap<>();

    public Map<String, ModelPrice> getPrices() {
        return prices;
    }

    public void setPrices(Map<String, ModelPrice> prices) {
        this.prices = prices;
    }

    public static class ModelPrice {
        /**
         * 每 1K 输入 Token 的价格（单位：元）
         */
        private BigDecimal inputPricePer1k = BigDecimal.ZERO;
        /**
         * 每 1K 输出 Token 的价格（单位：元）
         */
        private BigDecimal outputPricePer1k = BigDecimal.ZERO;

        public BigDecimal getInputPricePer1k() {
            return inputPricePer1k;
        }

        public void setInputPricePer1k(BigDecimal inputPricePer1k) {
            this.inputPricePer1k = inputPricePer1k;
        }

        public BigDecimal getOutputPricePer1k() {
            return outputPricePer1k;
        }

        public void setOutputPricePer1k(BigDecimal outputPricePer1k) {
            this.outputPricePer1k = outputPricePer1k;
        }
    }
}