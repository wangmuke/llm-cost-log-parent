package com.mukewang.ai.llmcost.autoconfigure;

import com.mukewang.ai.llmcost.autoconfigure.provider.DeepSeekCostProvider;
import com.mukewang.ai.llmcost.autoconfigure.provider.LlmCostProvider;
import com.mukewang.ai.llmcost.autoconfigure.provider.QwenCostProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class LlmCostAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public DeepSeekCostProvider deepSeekCostProvider() {
        return new DeepSeekCostProvider();
    }

    @Bean
    @ConditionalOnMissingBean
    public QwenCostProvider qwenCostProvider() {
        return new QwenCostProvider();
    }

    @Bean
    @ConditionalOnMissingBean
    public LlmCostLogger llmCostLogger(List<LlmCostProvider> providers) {
        return new LlmCostLogger(providers);
    }
}