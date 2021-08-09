package org.mytang.fairydust.spring;

import org.mytang.fairydust.core.FairyDustBootstrap;
import org.mytang.fairydust.core.FairyDustEnvironment;
import org.mytang.fairydust.core.service.dubbo.ConstantKey;
import org.mytang.fairydust.core.service.dubbo.GrayRule;
import org.mytang.fairydust.core.util.ReflectUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * @author tangmengyang
 */
@ConfigurationProperties(prefix = "fairydust.gray")
@ConditionalOnProperty(name = "spring.fairydust.enabled", matchIfMissing = true)
@EnableConfigurationProperties(FairyDustProperties.GrayRuleProperties.class)
public class FairyDustProperties {

    @Autowired
    public FairyDustProperties(GrayRuleProperties grayRuleProperties) {
        try {
            FairyDustEnvironment fairyDustEnvironment = FairyDustBootstrap.getInstance().getFairyDustEnvironment();
            if (grayRuleProperties.global != null) {
                fairyDustEnvironment.put(ConstantKey.GLOBAL,
                    ReflectUtils.declaredFieldMap(grayRuleProperties.global));
            }

            if (grayRuleProperties.services != null && !grayRuleProperties.services.isEmpty()) {
                fairyDustEnvironment.put(ConstantKey.SERVICES, grayRuleProperties.services.stream()
                    .filter(rule -> rule.getService() != null)
                    .collect(Collectors.toMap(GrayRule::getService, ReflectUtils::declaredFieldMap))
                );
            }

            if (grayRuleProperties.methods != null && !grayRuleProperties.methods.isEmpty()) {
                fairyDustEnvironment.put(ConstantKey.METHODS, grayRuleProperties.methods.stream()
                    .filter(rule -> rule.getService() != null && rule.getMethod() != null)
                    .collect(Collectors.toMap(rule -> rule.getService() + "#" + rule.getMethod(),
                        ReflectUtils::declaredFieldMap))
                );
            }
            FairyDustBootstrap.getInstance().bindSpy();
        } catch (Exception ignored) {
        }
    }

    public void setProperties(Map<String, Object> properties) {
        if (properties != null) {
            try {
                ConfigurationProperties configurationProperties =
                    this.getClass().getAnnotation(ConfigurationProperties.class);
                if (configurationProperties != null) {
                    String prefix = configurationProperties.prefix() + ".";
                    Map<String, Object> mapWithPrefix = new HashMap<>(properties.size());
                    properties.forEach((property, value) -> {
                        if (prefix.equals(property)) {
                            mapWithPrefix.put(property, value);
                        } else {
                            mapWithPrefix.put(prefix + property, value);
                        }
                    });
                    properties = mapWithPrefix;
                }

                FairyDustBootstrap.getInstance().initProxyEnvironment(properties);
            } catch (Exception ignored) {
            }
        }
    }

    @ConfigurationProperties(prefix = "fairydust.gray.rule")
    public static class GrayRuleProperties {

        GrayRule global;

        List<GrayRule> services;

        List<GrayRule> methods;

        public void setGlobal(GrayRule global) {
            this.global = global;
        }

        public void setServices(List<GrayRule> services) {
            this.services = services;
        }

        public void setMethods(List<GrayRule> methods) {
            this.methods = methods;
        }
    }
}
