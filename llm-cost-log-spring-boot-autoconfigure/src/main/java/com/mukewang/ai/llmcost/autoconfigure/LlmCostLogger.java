package com.mukewang.ai.llmcost.autoconfigure;

import com.mukewang.ai.llmcost.autoconfigure.provider.LlmCostProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class LlmCostLogger {

    private static final Logger log = LoggerFactory.getLogger(LlmCostLogger.class);

    private final List<LlmCostProvider> providers;

    public LlmCostLogger(List<LlmCostProvider> providers) {
        this.providers = providers;
    }

    public void logCost(String modelName, long inputTokens, long outputTokens) {
        logCost(modelName, inputTokens, outputTokens, null);
    }

    public void logCost(String modelName, long inputTokens, long outputTokens,
                        LlmCostProvider.CostContext context) {
        LlmCostProvider provider = providers.stream()
                .filter(p -> p.supports(modelName))
                .findFirst()
                .orElse(null);

        if (provider == null) {
            log.warn("[LLM-COST] 未找到模型 {} 的计价适配器，输入Token={}, 输出Token={}",
                    modelName, inputTokens, outputTokens);
            return;
        }

        LlmCostProvider.CostResult result = provider.calculate(modelName, inputTokens, outputTokens, context);

        log.info("[LLM-COST] provider={} model={} | inputTokens={} | outputTokens={} | inputCost={} | outputCost={} | totalCost={} | detail={}",
                provider.providerName(), modelName, inputTokens, outputTokens,
                result.getInputCost(), result.getOutputCost(), result.getTotalCost(), result.getDetail());
    }
}