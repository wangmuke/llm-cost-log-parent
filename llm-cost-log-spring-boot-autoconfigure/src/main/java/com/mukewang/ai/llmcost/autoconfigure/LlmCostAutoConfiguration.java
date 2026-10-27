package com.mukewang.ai.llmcost.autoconfigure;

import com.mukewang.ai.llmcost.autoconfigure.provider.DeepSeekCostProvider;
import com.mukewang.ai.llmcost.autoconfigure.provider.LlmCostProvider;
import com.mukewang.ai.llmcost.autoconfigure.provider.QwenCostProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@EnableConfigurationProperties(LlmPricingProperties.class)
public class LlmCostAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public DeepSeekCostProvider deepSeekCostProvider(LlmPricingProperties properties) {
        return new DeepSeekCostProvider(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public QwenCostProvider qwenCostProvider(LlmPricingProperties properties) {
        return new QwenCostProvider(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public LlmCostLogger llmCostLogger(List<LlmCostProvider> providers) {
        return new LlmCostLogger(providers);
    }
}