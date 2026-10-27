# llm-cost-log-spring-boot-starter

基于 Spring Boot 3 + Java 17 的大模型 Token 成本日志 Starter。对 DeepSeek、Qwen 等不同大模型的计价规则做统一抽象，业务代码只需传入模型名、输入 Token 数和输出 Token 数，Starter 自动按对应供应商的计价规则计算成本并输出结构化日志。

## 特性

- 统一入口 `LlmCostLogger`，按模型名自动路由到对应供应商的计价器
- 抽象接口 `LlmCostProvider`，新增供应商只需实现接口并注册 Bean
- 内置 DeepSeek 计价器，支持高峰/空闲时段、缓存命中/未命中
- 内置 Qwen 计价器，支持阶梯计价（按输入 Token 总量分档）
- 基于 Spring Boot 自动装配，引入依赖即可使用
- 单价按百万 Token 计算，BigDecimal 高精度，避免浮点误差

## 环境要求

- JDK 17+
- Spring Boot 3.x
- Maven 3.8+

## 模块说明

- `llm-cost-log-autoconfigure`：核心实现，包含计价器接口、各供应商实现、日志入口、自动配置类
- `llm-cost-log-spring-boot-starter`：空壳模块，聚合 autoconfigure 依赖，供业务方引入

## 安装

在父工程根目录执行：

```bash
mvn clean install -DskipTests
```

确保 `llm-cost-log-autoconfigure` 和 `llm-cost-log-spring-boot-starter` 都安装到本地 Maven 仓库。

## 引入依赖

在业务项目的 `pom.xml` 中加入：

```xml
<dependency>
    <groupId>com.mukewang.ai</groupId>
    <artifactId>llm-cost-log-spring-boot-starter</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

## 快速开始

### 1. 注入 LlmCostLogger

```java
import com.mukewang.ai.llmcost.autoconfigure.LlmCostLogger;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

    private final LlmCostLogger llmCostLogger;

    public ChatService(LlmCostLogger llmCostLogger) {
        this.llmCostLogger = llmCostLogger;
    }

    public void callDeepSeek() {
        // 调用大模型后拿到 token 用量
        long inputTokens = 1500;
        long outputTokens = 500;

        llmCostLogger.logCost("deepseek-flash", inputTokens, outputTokens);
    }
}
```

### 2. 传入计费上下文

DeepSeek 需要区分高峰/空闲、缓存命中；Qwen 需要按输入 Token 总量判断阶梯。通过 `LlmCostProvider.CostContext` 传入：

```java
import com.mukewang.ai.llmcost.autoconfigure.provider.LlmCostProvider;

LlmCostProvider.CostContext ctx = new LlmCostProvider.CostContext();
ctx.setPeakHour(true);      // DeepSeek：是否高峰时段
ctx.setCacheHit(false);     // DeepSeek：是否缓存命中
ctx.setInputTokenTotal(300_000L); // Qwen：输入 Token 总量，用于阶梯判断

llmCostLogger.logCost("deepseek-flash", 1500, 500, ctx);
llmCostLogger.logCost("qwen3.7-plus", 300_000, 8000, ctx);
```

不传 context 时按默认值处理：空闲时段、缓存未命中，Qwen 阶梯以单次 `inputTokens` 为准。

## 日志输出

正常输出示例：

```text
[LLM-COST] provider=DeepSeek model=deepseek-flash | inputTokens=1000000 | outputTokens=500000 | inputCost=1.00000000 | outputCost=2.00000000 | totalCost=3.00000000 | detail=空闲/缓存未命中
```

模型没有匹配到任何计价器时输出警告，不会抛异常：

```text
[LLM-COST] 未找到模型 unknown-model 的计价适配器，输入Token=100, 输出Token=100
```

## 内置计价规则

### DeepSeek

按百万 Token 计价，区分高峰/空闲和缓存命中/未命中。

deepseek-flash：

| 场景 | 输入单价（元/百万 Token） | 输出单价（元/百万 Token） |
|---|---|---|
| 空闲/缓存未命中 | 1 | 4 |
| 高峰/缓存未命中 | 2 | 8 |
| 空闲/缓存命中 | 0.02 | 4 |
| 高峰/缓存命中 | 0.04 | 8 |

deepseek-v4-pro：

| 场景 | 输入单价（元/百万 Token） | 输出单价（元/百万 Token） |
|---|---|---|
| 空闲/缓存未命中 | 4.5 | 13.5 |
| 高峰/缓存未命中 | 9.0 | 27.0 |
| 空闲/缓存命中 | 0.15 | 13.5 |
| 高峰/缓存命中 | 0.30 | 27.0 |

高峰时段为北京时间周一至周五 9:00-12:00、14:00-18:00，不含法定节假日。当前实现不自动判断高峰，由调用方通过 `CostContext.setPeakHour` 传入。

### Qwen

按输入 Token 总量阶梯计价，按百万 Token 单价。

qwen3.7-plus / qwen3.5-plus：

| 输入区间 | 输入单价（元/百万 Token） | 输出单价（元/百万 Token） |
|---|---|---|
| 0 < token ≤ 256K | 2 | 8 |
| 256K < token ≤ 1M | 6 | 24 |

qwen3.8-flash / qwen3.7-flash：

| 输入单价（元/百万 Token） | 输出单价（元/百万 Token） |
|---|---|
| 0.2 | 0.8 |

以上单价为示例值，实际使用请以厂商官方文档为准，并按需调整 `DeepSeekCostProvider` 和 `QwenCostProvider` 中的常量。

## 扩展新供应商

新增其他供应商只需三步。

### 1. 实现 LlmCostProvider 接口

```java
package com.mukewang.ai.llmcost.autoconfigure.provider;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class OpenAiCostProvider implements LlmCostProvider {

    @Override
    public boolean supports(String modelName) {
        return modelName != null && modelName.toLowerCase().startsWith("gpt");
    }

    @Override
    public String providerName() {
        return "OpenAI";
    }

    @Override
    public CostResult calculate(String modelName, long inputTokens, long outputTokens, CostContext context) {
        BigDecimal inputPerMillion = new BigDecimal("2");
        BigDecimal outputPerMillion = new BigDecimal("8");

        BigDecimal inputCost = inputPerMillion
                .multiply(BigDecimal.valueOf(inputTokens))
                .divide(BigDecimal.valueOf(1_000_000), 8, RoundingMode.HALF_UP);

        BigDecimal outputCost = outputPerMillion
                .multiply(BigDecimal.valueOf(outputTokens))
                .divide(BigDecimal.valueOf(1_000_000), 8, RoundingMode.HALF_UP);

        return new CostResult(inputCost, outputCost, "标准计价");
    }
}
```

### 2. 在自动配置类中注册 Bean

```java
@Bean
@ConditionalOnMissingBean
public OpenAiCostProvider openAiCostProvider() {
    return new OpenAiCostProvider();
}
```

### 3. 调用方无需改动

`LlmCostLogger` 会自动发现新增的 `LlmCostProvider` Bean，按 `supports` 方法路由。

## 测试

在父工程根目录执行：

```bash
mvn clean test
```

或在 IDE 中对 `llm-cost-log-autoconfigure` 模块的 test 目录运行全部测试。

测试覆盖：

- DeepSeek 各场景计价（空闲/高峰、缓存命中/未命中、v4-pro、空上下文默认值）
- Qwen 阶梯边界（256K 整、256K+1、上下文总量优先）
- `LlmCostLogger` 按模型名路由到正确 Provider
- 未知模型、空 Provider 列表不抛异常
- Spring 容器自动装配 Bean 注册

## 注意事项

- 单价单位为元/百万 Token，与厂商官方报价一致，计算时除以 1_000_000
- 价格硬编码在各 Provider 中，厂商调价时需修改源码；如需动态配置可改为从配置文件读取
- DeepSeek 高峰时段不自动判断，由调用方传入 `peakHour`
- Qwen 阶梯以 `inputTokenTotal`（若提供）或单次 `inputTokens` 为判断依据，分档边界为左闭右开
- 日志使用 SLF4J，业务方按需配置日志级别和输出目标