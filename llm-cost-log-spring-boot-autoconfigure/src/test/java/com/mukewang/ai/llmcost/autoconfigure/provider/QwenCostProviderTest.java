package com.mukewang.ai.llmcost.autoconfigure.provider;

import com.mukewang.ai.llmcost.autoconfigure.LlmPricingProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QwenCostProviderTest {

    private final QwenCostProvider provider = new QwenCostProvider(new LlmPricingProperties());

    @Test
    void supports_qwenModels() {
        assertThat(provider.supports("qwen3.7-plus")).isTrue();
        assertThat(provider.supports("qwq-32b")).isTrue();
        assertThat(provider.supports("deepseek-flash")).isFalse();
        assertThat(provider.supports(null)).isFalse();
    }

    @Test
    void plus_within256K_tier1() {
        // 输入 ≤256K：2元/M，输出 8元/M
        LlmCostProvider.CostResult r = provider.calculate("qwen3.7-plus", 200_000, 8_000, null);

        assertThat(r.getInputCost()).isEqualByComparingTo("0.40000000");
        assertThat(r.getOutputCost()).isEqualByComparingTo("0.06400000");
        assertThat(r.getTotalCost()).isEqualByComparingTo("0.46400000");
        assertThat(r.getDetail()).isEqualTo("≤256K");
    }

    @Test
    void plus_over256K_tier2() {
        // 输入 >256K：6元/M，输出 24元/M
        LlmCostProvider.CostContext ctx = new LlmCostProvider.CostContext();
        ctx.setInputTokenTotal(300_000L);

        LlmCostProvider.CostResult r = provider.calculate("qwen3.7-plus", 300_000, 8_000, ctx);

        assertThat(r.getInputCost()).isEqualByComparingTo("1.80000000");
        assertThat(r.getOutputCost()).isEqualByComparingTo("0.19200000");
        assertThat(r.getTotalCost()).isEqualByComparingTo("1.99200000");
        assertThat(r.getDetail()).isEqualTo("256K~1M");
    }

    @Test
    void exactly256K_isTier1() {
        LlmCostProvider.CostResult r = provider.calculate("qwen3.7-plus", 256_000, 0, null);
        assertThat(r.getDetail()).isEqualTo("≤256K");
    }

    @Test
    void exactly256K_plus1_isTier2() {
        LlmCostProvider.CostResult r = provider.calculate("qwen3.7-plus", 256_001, 0, null);
        assertThat(r.getDetail()).isEqualTo("256K~1M");
    }

    @Test
    void flashModel() {
        LlmCostProvider.CostResult r = provider.calculate("qwen3.8-flash", 1_000_000, 500_000, null);
        assertThat(r.getInputCost()).isEqualByComparingTo("0.20000000");
        assertThat(r.getOutputCost()).isEqualByComparingTo("0.40000000");
        assertThat(r.getDetail()).isEqualTo("Flash档");
    }

    @Test
    void contextInputTokenTotal_takesPrecedence() {
        // 单次 inputTokens 小，但上下文总量超过 256K，应走阶梯 2
        LlmCostProvider.CostContext ctx = new LlmCostProvider.CostContext();
        ctx.setInputTokenTotal(400_000L);
        LlmCostProvider.CostResult r = provider.calculate("qwen3.7-plus", 10_000, 0, ctx);
        assertThat(r.getDetail()).isEqualTo("256K~1M");
    }
}