# 使用方式
目前支持两种使用方式：Spring Boot 和 Java Agent

方式|优点|缺点
----|----|----
Spring Boot|不用关心应用运维部署的方式，完全 Spring 式的配置，同时可享受 Apollo 配置中心的动态控制|入侵业务应用（需主动引用）
Java Agent|应用完全无感知，随时使用随时移除|需要运维同事配合将 Agent 上传，同时将不同版本的参数配置分开


# Spring Boot
当前各团队应用基本采用 Spring Boot 开发，所以我们按 Spring Boot Starter 的方式提供 Jar 包。直接添加如下引用即可：
```xml
<dependency>
    <groupId>org.mytang</groupId>
    <artifactId>fairydust-spring-starter</artifactId>
    <version>LATEST</version>
</dependency>
```
<font color=red>**如果你用其他使用方式需求，请联系告知，谢谢！**</font>

### 配置
配置优先级同你的应用一样，所以不用担心

#### 基础配置
因为服务通过 Jar 包的方式嵌入了你的应用，所以在你有需求时显示打开才能生效；未开启时，应用不会为此创建任何的 Bean，**所以配置后请重启应用**

参数名|描述|值域
----|----|----
spring.fairydust.enabled|是否启用代理|true/false
spring.fairydust.silentInit|是否安静启动；当设置安静启动时，代理启动过程中出现的异常不会影响到应用，否则会抛出异常|true/false

#### 灰度配置
在灰度场景是有两类服务的，灰度服务和被代理服务。我们的配置是**互斥的**：只要配置了灰度服务参数，被代理服务参数就会失效

- 灰度服务

即目标服务，是一个隔离的服务，需要代理转发才能获取到流量

- 被代理服务

及正常服务，通过开启代理和配置规则，让部分流量转发到【灰度服务】

- 灰度配置

参数名|描述|样例
----|----|----
fairydust.gray.group|灰度服务的 Dubbo 服务分组|gray
fairydust.gray.version|灰度服务的 Dubbo 服务版本|9.9.9


#### 规则配置
规则分为 3 类：全局规则、接口规则、方法规则，生效优先级为：方法规则 > 接口规则 > 全局规则

下面分别介绍：

-  全局规则

参数名|描述|样例
----|----|----
fairydust.gray.rule.global.condition|[生效条件](#生效条件)|true
fairydust.gray.rule.global.group|Dubbo 目标服务分组|默认为：gray
fairydust.gray.rule.global.version|Dubbo 目标服务版本|默认为：9.9.9

-  接口规则

参数名|描述|样例
----|----|----
fairydust.gray.rule.services[0].service|接口|org.mytang.service.XXXService
fairydust.gray.rule.services[0].condition|[生效条件](#生效条件)|true
fairydust.gray.rule.services[0].group|Dubbo 目标服务分组|默认为：gray
fairydust.gray.rule.services[0].version|Dubbo 目标服务版本|默认为：9.9.9


<font color=red>*多个请依次填写*</font>


-  方法规则

参数名|描述|样例
----|----|----
fairydust.gray.rule.methods[0].service|接口|org.mytang.service.XXXService
fairydust.gray.rule.methods[0].method|方法名（后续可以改为方法签名）|XXXMethod
fairydust.gray.rule.methods[0].condition|[生效条件](#生效条件)|true
fairydust.gray.rule.methods[0].group|Dubbo 目标服务分组|默认为：gray
fairydust.gray.rule.methods[0].version|Dubbo 目标服务版本|默认为：9.9.9

<font color=red>*多个请依次填写*</font>


# Java Agent

```bash
# ABSOLUTE_PATH = fairydust-agent.jar 所在绝对路径
# ARGS = 你想要传入的参数
-javaagent:${ABSOLUTE_PATH}/fairydust-agent.jar=${ARGS}
```

### 样例
```bash
# 灰度应用
java -jar -Dspring.profiles.active=test -javaagent:fairydust-agent.jar=gray.group%3Dgray%3Bgray.version%3D9.9.9 demo.jar

# 稳定版本
java -jar -Dspring.profiles.active=test -javaagent:fairydust-agent.jar=gray.rule.global%3D%7B%22condition%22%3A%22true%22%7D demo.jar
```

### 配置

通过 URL 的方式填写 `p1=v1&p2=v3&...&pn=vn`

**提醒：**为了避免特殊字符的使用问题，建议你将参数整体用 URL Encode 编码


#### 基础配置

Agent 是一种主动使用方式，所以无需配置

#### 灰度配置
参见 Spring Boot

#### 规则配置
参见 Spring Boot 的配置 JSON 化即可，例如：
```
fairydust.gray.rule.global={"condition":"true"}
fairydust.gray.rule.services=[{"service":"XXXService","condition":"true"}]
fairydust.gray.rule.methods=[{"service":"XXXService","method":"XXXMethod",condition":"true"}]
```

s
# 生效条件
目前采用的表达式引擎为：QLExpress，详见：https://github.com/alibaba/QLExpress

**如果你有更优的引擎方案烦请告知，非常感谢！**

特别提醒——能操作的变量（或者叫参数）是有限的，列举如下：

变量|描述
----|----
service|接口名
method|方法名
$0,$1,...,$n|方法入参，能轻松的操作单个参数内部属性，预发参见 QLExpress
