package com.mukewang.ai.llmcost.autoconfigure.provider;

import com.mukewang.ai.llmcost.autoconfigure.LlmPricingProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DeepSeekCostProviderTest {

    private final DeepSeekCostProvider provider = new DeepSeekCostProvider(new LlmPricingProperties());

    @Test
    void supports_deepseekModels() {
        assertThat(provider.supports("deepseek-flash")).isTrue();
        assertThat(provider.supports("deepseek-v4-pro")).isTrue();
        assertThat(provider.supports("DeepSeek-Chat")).isTrue();
        assertThat(provider.supports("qwen3.7-plus")).isFalse();
        assertThat(provider.supports(null)).isFalse();
    }

    @Test
    void flash_idle_noCache() {
        // 空闲 + 缓存未命中：输入 1元/M，输出 4元/M
        LlmCostProvider.CostContext ctx = new LlmCostProvider.CostContext();
        ctx.setPeakHour(false);
        ctx.setCacheHit(false);

        LlmCostProvider.CostResult r = provider.calculate("deepseek-flash", 1_000_000, 500_000, ctx);

        assertThat(r.getInputCost()).isEqualByComparingTo("1.00000000");
        assertThat(r.getOutputCost()).isEqualByComparingTo("2.00000000");
        assertThat(r.getTotalCost()).isEqualByComparingTo("3.00000000");
        assertThat(r.getDetail()).isEqualTo("空闲/缓存未命中");
    }

    @Test
    void flash_peak_noCache() {
        // 高峰 + 缓存未命中：输入 2元/M，输出 8元/M
        LlmCostProvider.CostContext ctx = new LlmCostProvider.CostContext();
        ctx.setPeakHour(true);
        ctx.setCacheHit(false);

        LlmCostProvider.CostResult r = provider.calculate("deepseek-flash", 1_000_000, 500_000, ctx);

        assertThat(r.getInputCost()).isEqualByComparingTo("2.00000000");
        assertThat(r.getOutputCost()).isEqualByComparingTo("4.00000000");
        assertThat(r.getTotalCost()).isEqualByComparingTo("6.00000000");
        assertThat(r.getDetail()).isEqualTo("高峰/缓存未命中");
    }

    @Test
    void flash_cacheHit() {
        // 高峰 + 缓存命中：输入 0.04元/M
        LlmCostProvider.CostContext ctx = new LlmCostProvider.CostContext();
        ctx.setPeakHour(true);
        ctx.setCacheHit(true);

        LlmCostProvider.CostResult r = provider.calculate("deepseek-flash", 1_000_000, 0, ctx);

        assertThat(r.getInputCost()).isEqualByComparingTo("0.04000000");
        assertThat(r.getDetail()).isEqualTo("高峰/缓存命中");
    }

    @Test
    void v4pro_idle_noCache() {
        // v4-pro 空闲：输入 4.5元/M，输出 13.5元/M
        LlmCostProvider.CostContext ctx = new LlmCostProvider.CostContext();

        LlmCostProvider.CostResult r = provider.calculate("deepseek-v4-pro", 2_000_000, 1_000_000, ctx);

        assertThat(r.getInputCost()).isEqualByComparingTo("9.00000000");
        assertThat(r.getOutputCost()).isEqualByComparingTo("13.50000000");
        assertThat(r.getTotalCost()).isEqualByComparingTo("22.50000000");
    }

    @Test
    void nullContext_defaultsToIdleNoCache() {
        LlmCostProvider.CostResult r = provider.calculate("deepseek-flash", 1_000_000, 0, null);
        assertThat(r.getInputCost()).isEqualByComparingTo("1.00000000");
        assertThat(r.getDetail()).isEqualTo("空闲/缓存未命中");
    }
}