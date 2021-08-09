package org.mytang.fairydust.spring;

import org.mytang.fairydust.core.FairyDustBootstrap;
import java.lang.instrument.Instrumentation;
import java.util.HashMap;
import java.util.Map;
import net.bytebuddy.agent.ByteBuddyAgent;

/**
 * @author tangmengyang
 */
public final class FairyDustAgent {

    private String errorMessage;

    private Map<String, Object> configMap = new HashMap<>();

    private Instrumentation instrumentation;

    private boolean silentInit;

    public FairyDustAgent(boolean silentInit, Map<String, Object> configMap, Instrumentation instrumentation) {

        this.silentInit = silentInit;

        if (configMap != null) {
            this.configMap = configMap;
        }

        this.instrumentation = instrumentation;
    }

    public void init() throws IllegalStateException {
        try {
            if (instrumentation == null) {
                instrumentation = ByteBuddyAgent.install();
            }

            FairyDustBootstrap.getInstance(instrumentation, configMap);
        } catch (Throwable e) {
            errorMessage = e.getMessage();

            if (!silentInit) {
                throw new IllegalStateException(e);
            }
        }
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
