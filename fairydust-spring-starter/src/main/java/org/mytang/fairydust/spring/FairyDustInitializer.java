package org.mytang.fairydust.spring;

import org.mytang.fairydust.core.service.dubbo.ConstantKey;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * @author tangmengyang
 */
public class FairyDustInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final Logger log = LoggerFactory.getLogger(FairyDustInitializer.class);

    private static final String ENABLED = "spring.fairydust.enabled";
    private static final String SILENT = "spring.fairydust.silentInit";

    private ConfigurableEnvironment environment;

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {

        this.environment = applicationContext.getEnvironment();

        Boolean enabled = environment.getProperty(ENABLED, Boolean.class, Boolean.FALSE);
        if (!enabled) {
            return;
        }

        Map<String, Object> configMap = initConfigMap();

        Boolean silentInit = environment.getProperty(SILENT, Boolean.class, Boolean.FALSE);

        final FairyDustAgent fairyDustAgent = new FairyDustAgent(silentInit, configMap, null);

        fairyDustAgent.init();

        String errorMessage = fairyDustAgent.getErrorMessage();
        if (errorMessage != null) {
            log.info("fairydust agent start fail. {}", errorMessage);
        } else {
            log.info("fairydust agent start success.");
        }
    }

    private Map<String, Object> initConfigMap() {
        Map<String, Object> configMap = new HashMap<>(4);

        Optional.ofNullable(environment.getProperty(ConstantKey.GRAY_GROUP)).ifPresent(
            value -> configMap.put(ConstantKey.GRAY_GROUP, value)
        );

        Optional.ofNullable(environment.getProperty(ConstantKey.GRAY_VERSION)).ifPresent(
            value -> configMap.put(ConstantKey.GRAY_VERSION, value)
        );

        return configMap;
    }
}
