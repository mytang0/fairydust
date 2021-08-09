package org.mytang.fairydust.core;

import java.fairydust.Spy;
import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.security.CodeSource;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarFile;
import org.mytang.bridge.Bridge;
import org.mytang.fairydust.core.service.dubbo.DubboTransformer;
import org.mytang.fairydust.core.util.FeatureCodec;

/**
 * @author tangmengyang
 */
public class FairyDustBootstrap {

    private static volatile FairyDustBootstrap fairyDustBootstrap;

    private Instrumentation instrumentation;

    private TransformerManager transformerManager;

    private FairyDustEnvironment fairyDustEnvironment;

    private FairyDustBootstrap(Instrumentation instrumentation, Map<String, Object> args)
        throws Throwable {

        this.instrumentation = instrumentation;

        this.initSpy();

        if (Spy.isInited()) {
            return;
        }

        this.initProxyEnvironment(args);

        Runtime.getRuntime().addShutdownHook(
            new Thread("fairydust-shutdown-hooker") {
                @Override
                public void run() {
                    FairyDustBootstrap.this.destroy();
                }
            }
        );

        this.transformerManager = new TransformerManager(instrumentation);
        this.transformerManager.addTransformer(new DubboTransformer(fairyDustEnvironment));

        this.bindSpy();
    }

    private void initSpy() throws Throwable {
        ClassLoader parent = ClassLoader.getSystemClassLoader().getParent();
        Class<?> spyClass = null;
        if (parent != null) {
            try {
                spyClass = parent.loadClass("java.fairydust.Spy");
            } catch (Throwable ignored) {
            }
        }
        if (spyClass == null) {
            CodeSource codeSource = Bridge.class.getProtectionDomain().getCodeSource();
            if (codeSource != null) {
                File spyJarFile = new File(codeSource.getLocation().toURI().getSchemeSpecificPart());
                instrumentation.appendToBootstrapClassLoaderSearch(new JarFile(spyJarFile));
            } else {
                throw new IllegalStateException("can not find java.fairydust.SpyAPI");
            }
        }
    }

    public void initProxyEnvironment(Map<String, Object> argsMap) throws IOException {
        getFairyDustEnvironment();

        Map<String, String> systemEnvironment = System.getenv();
        systemEnvironment.forEach((property, value) -> {

            if (Constant.PROPERTY_PREFIX.equals(property)) {
                fairyDustEnvironment.put(property, value);
            }
        });

        Properties systemProperties = System.getProperties();
        systemProperties.forEach((property, value) -> {

            if (property instanceof String && Constant.PROPERTY_PREFIX.equals(property)) {
                fairyDustEnvironment.put((String) property, value);
            }
        });

        if (argsMap != null && !argsMap.isEmpty()) {
            fairyDustEnvironment.putAll(removeDashKey(argsMap));
        }
    }

    public void bindSpy() {
        Spy.getEnvironment()
            .putAll(fairyDustEnvironment.getProperties());

        Spy.init();
    }

    private void destroy() {
        if (transformerManager != null) {
            transformerManager.destroy();
        }
    }

    public synchronized static FairyDustBootstrap getInstance(Instrumentation instrumentation, String args)
        throws Throwable {

        if (fairyDustBootstrap != null) {
            return fairyDustBootstrap;
        }

        Map<String, String> argsMap = FeatureCodec.DEFAULT_COMMANDLINE_CODEC.toMap(args);

        Map<String, Object> mapWithPrefix = new HashMap<>(argsMap.size());
        for (Map.Entry<String, String> entry : argsMap.entrySet()) {
            if (Constant.PROPERTY_PREFIX.equals(entry.getKey())) {
                mapWithPrefix.put(entry.getKey(), entry.getValue());
            } else {
                mapWithPrefix.put(Constant.PROPERTY_PREFIX + entry.getKey(), entry.getValue());
            }
        }

        return getInstance(instrumentation, mapWithPrefix);
    }

    public synchronized static FairyDustBootstrap getInstance(Instrumentation instrumentation, Map<String, Object> args)
        throws Throwable {
        if (fairyDustBootstrap == null) {
            fairyDustBootstrap = new FairyDustBootstrap(instrumentation, args);
        }
        return fairyDustBootstrap;
    }

    public static FairyDustBootstrap getInstance() {
        if (fairyDustBootstrap == null) {
            throw new IllegalStateException("FairyDustBootstrap must be initialized before!");
        }
        return fairyDustBootstrap;
    }

    public Instrumentation getInstrumentation() {
        return instrumentation;
    }

    public void setInstrumentation(Instrumentation instrumentation) {
        this.instrumentation = instrumentation;
    }

    public TransformerManager getTransformerManager() {
        return transformerManager;
    }

    public FairyDustEnvironment getFairyDustEnvironment() {
        if (fairyDustEnvironment == null) {
            fairyDustEnvironment = new FairyDustEnvironment();
        }
        return fairyDustEnvironment;
    }

    private static Map<String, Object> removeDashKey(Map<String, Object> map) {

        Map<String, Object> result = new HashMap<>(map.size());

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();

            if (key.contains("-")) {
                StringBuilder sb = new StringBuilder(key.length());
                for (int i = 0; i < key.length(); i++) {
                    if (key.charAt(i) == '-' && (i + 1 < key.length()) && Character.isAlphabetic(key.charAt(i + 1))) {
                        ++i;
                        char upperChar = Character.toUpperCase(key.charAt(i));
                        sb.append(upperChar);
                    } else {
                        sb.append(key.charAt(i));
                    }
                }
                key = sb.toString();
            }

            result.put(key, entry.getValue());
        }
        return result;
    }
}
