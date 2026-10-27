# llm-cost-log-spring-boot-starter

基于 Spring Boot 3 + Java 17 的大模型 Token 成本日志 Starter。对 DeepSeek、Qwen 等不同大模型的计价规则做统一抽象，业务代码只需传入模型名、输入 Token 数和输出 Token 数，Starter 自动按对应供应商的计价规则计算成本并输出结构化日志。单价支持通过配置文件覆盖，未配置时回落到各供应商内置默认值。

## 特性

- 统一入口 `LlmCostLogger`，按模型名自动路由到对应供应商的计价器
- 抽象接口 `LlmCostProvider`，新增供应商只需实现接口并注册 Bean
- 内置 DeepSeek 计价器，支持高峰/空闲时段、缓存命中/未命中
- 内置 Qwen 计价器，支持阶梯计价（按输入 Token 总量分档）
- 单价可通过 `application.yml` 的 `llm.cost.prices` 覆盖，未配置字段回落到内置默认值
- 基于 Spring Boot 自动装配，引入依赖即可使用
- 单价按百万 Token 计算，BigDecimal 高精度，避免浮点误差

## 环境要求

- JDK 17+
- Spring Boot 3.x
- Maven 3.8+

## 模块说明

- `llm-cost-log-autoconfigure`：核心实现，包含计价器接口、各供应商实现、日志入口、配置属性、自动配置类
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

正常输出示例，走内置单价时 `detail` 带 `(内置)`：

```text
[LLM-COST] provider=DeepSeek model=deepseek-flash | inputTokens=1000000 | outputTokens=500000 | inputCost=1.00000000 | outputCost=2.00000000 | totalCost=3.00000000 | detail=空闲/缓存未命中(内置)
```

配置覆盖生效时 `detail` 带 `(配置)`：

```text
[LLM-COST] provider=DeepSeek model=deepseek-flash | inputTokens=1000000 | outputTokens=500000 | inputCost=2.00000000 | outputCost=4.00000000 | totalCost=6.00000000 | detail=高峰/缓存未命中(配置)
```

模型没有匹配到任何计价器时输出警告，不会抛异常：

```text
[LLM-COST] 未找到模型 unknown-model 的计价适配器，输入Token=100, 输出Token=100
```

## 单价配置

### 配置覆盖优先级

1. `application.yml` 中 `llm.cost.prices` 显式配置的单价
2. 各 Provider 内置默认单价

配置 key 支持精确匹配和前缀匹配：`findPrice` 先按模型名精确查找，未命中再按前缀匹配。例如配置 `deepseek` 可覆盖所有以 `deepseek` 开头的模型。

### 配置字段说明

`LlmPricingProperties.ModelPrice` 支持以下字段，均为可选，单位为元/百万 Token：

| 字段 | 含义 | 适用场景 |
|---|---|---|
| inputPricePerMillion | 单一输入单价 | 通用兜底 |
| outputPricePerMillion | 单一输出单价 | 通用兜底 |
| peakInputPricePerMillion | 高峰输入单价 | DeepSeek |
| peakOutputPricePerMillion | 高峰输出单价 | DeepSeek |
| idleInputPricePerMillion | 空闲输入单价 | DeepSeek |
| idleOutputPricePerMillion | 空闲输出单价 | DeepSeek |
| cacheHitInputPricePerMillion | 缓存命中输入单价 | DeepSeek |

DeepSeek Provider 取值顺序：高峰取 `peak*`，空闲取 `idle*`，缓存命中时 `cacheHitInputPricePerMillion` 覆盖输入单价；上述字段缺省时回落到 `inputPricePerMillion` / `outputPricePerMillion`；仍缺省则走内置默认。

Qwen Provider 取值顺序：`inputPricePerMillion` 和 `outputPricePerMillion` 同时存在时直接使用，否则走内置阶梯规则。

### 配置示例

```yaml
llm:
  cost:
    prices:
      # 精确覆盖 deepseek-flash 的四个场景
      deepseek-flash:
        peak-input-price-per-million: 2.00
        peak-output-price-per-million: 8.00
        idle-input-price-per-million: 1.00
        idle-output-price-per-million: 4.00
        cache-hit-input-price-per-million: 0.03

      # 前缀覆盖所有 deepseek 模型，单一单价兜底
      deepseek:
        input-price-per-million: 1.5
        output-price-per-million: 5.0

      # 精确覆盖 qwen3.7-plus
      qwen3.7-plus:
        input-price-per-million: 2.0
        output-price-per-million: 8.0
```

## 内置计价规则

未在配置文件覆盖时使用以下内置默认值。

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

以上单价为示例值，实际使用请以厂商官方文档为准，可通过配置文件覆盖或调整 Provider 中的常量。

## 扩展新供应商

新增其他供应商只需三步。

### 1. 实现 LlmCostProvider 接口

```java
package com.mukewang.ai.llmcost.autoconfigure.provider;

import com.mukewang.ai.llmcost.autoconfigure.LlmPricingProperties;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class OpenAiCostProvider implements LlmCostProvider {

    private final LlmPricingProperties properties;

    public OpenAiCostProvider(LlmPricingProperties properties) {
        this.properties = properties;
    }

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
        String detail = "标准计价(内置)";

        LlmPricingProperties.ModelPrice cfg = properties.findPrice(modelName);
        if (cfg != null && cfg.getInputPricePerMillion() != null && cfg.getOutputPricePerMillion() != null) {
            inputPerMillion = cfg.getInputPricePerMillion();
            outputPerMillion = cfg.getOutputPricePerMillion();
            detail = "标准计价(配置)";
        }

        BigDecimal inputCost = inputPerMillion
                .multiply(BigDecimal.valueOf(inputTokens))
                .divide(BigDecimal.valueOf(1_000_000), 8, RoundingMode.HALF_UP);

        BigDecimal outputCost = outputPerMillion
                .multiply(BigDecimal.valueOf(outputTokens))
                .divide(BigDecimal.valueOf(1_000_000), 8, RoundingMode.HALF_UP);

        return new CostResult(inputCost, outputCost, detail);
    }
}
```

### 2. 在自动配置类中注册 Bean

```java
@Bean
@ConditionalOnMissingBean
public OpenAiCostProvider openAiCostProvider(LlmPricingProperties properties) {
    return new OpenAiCostProvider(properties);
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

- DeepSeek 各场景内置计价（空闲/高峰、缓存命中/未命中、v4-pro、空上下文默认值）
- DeepSeek 配置覆盖生效，`detail` 标记为 `(配置)`
- Qwen 阶梯边界（256K 整、256K+1、上下文总量优先）
- Qwen 配置覆盖生效
- `LlmCostLogger` 按模型名路由到正确 Provider
- 未知模型、空 Provider 列表不抛异常
- Spring 容器自动装配 Bean 注册

注意：Provider 构造已改为注入 `LlmPricingProperties`，测试中构造 Provider 需传入实例：

```java
DeepSeekCostProvider provider = new DeepSeekCostProvider(new LlmPricingProperties());
```

配置覆盖测试示例：

```java
@Test
void configOverridesBuiltInPrice() {
    LlmPricingProperties props = new LlmPricingProperties();
    LlmPricingProperties.ModelPrice mp = new LlmPricingProperties.ModelPrice();
    mp.setPeakInputPricePerMillion(new BigDecimal("100"));
    mp.setPeakOutputPricePerMillion(new BigDecimal("200"));
    props.getPrices().put("deepseek-flash", mp);

    DeepSeekCostProvider provider = new DeepSeekCostProvider(props);
    LlmCostProvider.CostContext ctx = new LlmCostProvider.CostContext();
    ctx.setPeakHour(true);

    LlmCostProvider.CostResult r = provider.calculate("deepseek-flash", 1_000_000, 1_000_000, ctx);
    assertThat(r.getInputCost()).isEqualByComparingTo("100.00000000");
    assertThat(r.getOutputCost()).isEqualByComparingTo("200.00000000");
    assertThat(r.getDetail()).contains("配置");
}
```

## 注意事项

- 单价单位为元/百万 Token，与厂商官方报价一致，计算时除以 1_000_000
- 配置 key 精确匹配优先于前缀匹配，`prices` 为空时不报错，直接走内置默认
- 配置文件只需覆盖需要调整的字段，未配置字段回落到内置默认值
- DeepSeek 高峰时段不自动判断，由调用方传入 `peakHour`
- Qwen 阶梯以 `inputTokenTotal`（若提供）或单次 `inputTokens` 为判断依据，分档边界为左闭右开
- 日志使用 SLF4J，业务方按需配置日志级别和输出目标