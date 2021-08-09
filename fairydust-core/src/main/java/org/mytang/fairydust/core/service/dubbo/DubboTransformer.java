package org.mytang.fairydust.core.service.dubbo;

import javassist.NotFoundException;
import org.mytang.fairydust.core.AbstractTransformer;
import org.mytang.fairydust.core.FairyDustEnvironment;
import org.mytang.fairydust.core.service.GrayContext;
import org.mytang.fairydust.core.util.ClassGenerator;
import org.mytang.fairydust.core.util.ExpressUtils;
import org.mytang.fairydust.core.util.ReflectUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.fairydust.Spy;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtMethod;

/**
 * @author tangmengyang
 */
public class DubboTransformer extends AbstractTransformer {

    /**
     * dubbo 从 alibaba 转入 apache 孵化之后，所有的包被重命名
     */
    private static final String IS_APACHE_DUBBO = "is.apache.dubbo";

    private static final String EXPORT_CLASS = "com/alibaba/dubbo/config/ServiceConfig";
    private static final String APACHE_EXPORT_CLASS = "org/apache/dubbo/config/ServiceConfig";
    private static final String APACHE_PROXY_CLASS = "org/apache/dubbo/config/ServiceConfigBase";
    private static final String EXPORT_METHOD = "export";
    private static final String PROXY_KEY_METHOD = "setRef";

    private static final String REFERENCE_CLASS = "com/alibaba/dubbo/config/ReferenceConfig";
    private static final String APACHE_REFERENCE_CLASS = "org/apache/dubbo/config/ReferenceConfig";
    private static final String REFERENCE_METHOD = "init";

    private static final String SPRING_BOOT_CLASS_LOADER = "org/springframework/boot/loader/LaunchedURLClassLoader";

    private static final Map<ClassLoader, Object> REFEREE_MAP = new ConcurrentHashMap<>();

    private static final Boolean PLACEHOLDER = Boolean.TRUE;

    private static final Map<Class<?>, Boolean> RETRY_LIMIT_MAP = new ConcurrentHashMap<>();

    private static final Map<String, Object> REFERENCE_MAP = new ConcurrentHashMap<>();

    private static final ScheduledExecutorService RETRY_EXECUTOR = Executors.newSingleThreadScheduledExecutor();

    static {
        RETRY_EXECUTOR.scheduleAtFixedRate(RETRY_LIMIT_MAP::clear, 10, 300, TimeUnit.SECONDS);
    }

    public DubboTransformer(FairyDustEnvironment proxyEnvironment) {
        super(proxyEnvironment);
    }

    @Override
    public void environmentResolve(FairyDustEnvironment fairyDustEnvironment) {
        Object global = fairyDustEnvironment.getObject(ConstantKey.GLOBAL);
        if (global instanceof String) {
            try {
                fairyDustEnvironment.put(ConstantKey.GLOBAL,
                    ReflectUtils.declaredFieldMap(JSON.parseObject((String) global, GrayRule.class)));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        Object services = fairyDustEnvironment.getObject(ConstantKey.SERVICES);
        if (services instanceof String) {
            try {
                List<GrayRule> serviceRules = JSON.parseObject((String) services, new TypeReference<List<GrayRule>>() {
                });
                if (serviceRules != null) {
                    fairyDustEnvironment.put(ConstantKey.SERVICES, serviceRules.stream()
                        .filter(rule -> rule.getService() != null)
                        .collect(Collectors.toMap(GrayRule::getService, ReflectUtils::declaredFieldMap)));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        Object methods = fairyDustEnvironment.getObject(ConstantKey.METHODS);
        if (methods instanceof String) {
            try {
                List<GrayRule> methodRules = JSON.parseObject((String) methods, new TypeReference<List<GrayRule>>() {
                });
                if (methodRules != null) {
                    fairyDustEnvironment.put(ConstantKey.METHODS, methodRules.stream()
                        .filter(rule -> rule.getService() != null && rule.getMethod() != null)
                        .collect(Collectors.toMap(rule -> rule.getService() + "#" + rule.getMethod(), ReflectUtils::declaredFieldMap)));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
        ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {

        if (EXPORT_CLASS.equals(className) || APACHE_EXPORT_CLASS.equals(className) || APACHE_PROXY_CLASS.equals(className)) {

            try (InputStream classfile = new ByteArrayInputStream(classfileBuffer)) {
                return Optional.ofNullable(enhancer(className, classfile)).orElse(classfileBuffer);
            } catch (Exception e) {
                e.printStackTrace();
            }

        } else if (REFERENCE_CLASS.equals(className) || APACHE_REFERENCE_CLASS.equals(className)) {

            String grayGroup = fairyDustEnvironment.getString(ConstantKey.GRAY_GROUP);

            if (grayGroup == null) {
                try (InputStream classfile = new ByteArrayInputStream(classfileBuffer)) {
                    return Optional.ofNullable(referenceEnhancer(classfile)).orElse(classfileBuffer);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else if (SPRING_BOOT_CLASS_LOADER.equals(className)) {

            try (InputStream classfile = new ByteArrayInputStream(classfileBuffer)) {
                return Optional.ofNullable(springBootClassLoaderEnhancer(classfile)).orElse(classfileBuffer);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return classfileBuffer;
    }

    private byte[] enhancer(String className, InputStream classfile) throws Exception {

        String grayGroup = fairyDustEnvironment.getString(ConstantKey.GRAY_GROUP);

        if (grayGroup != null) {
            String grayVersion = fairyDustEnvironment.getString(ConstantKey.GRAY_VERSION, GrayRule.GRAY_VERSION);

            boolean isApacheProxy = APACHE_PROXY_CLASS.equals(className);
            return !isApacheProxy ? exportEnhancer(classfile, grayGroup, grayVersion) : null;
        } else {

            boolean isApacheExport = APACHE_EXPORT_CLASS.equals(className);
            if (isApacheExport) {
                Spy.getEnvironment().put(IS_APACHE_DUBBO, Boolean.TRUE);
            }
            return !isApacheExport ? proxyEnhancer(classfile) : null;
        }
    }

    private static byte[] exportEnhancer(InputStream classfile, String grayGroup, String grayVersion) throws Exception {

        CtClass ctClass = null;

        try {
            ctClass = ClassGenerator.getClassPool(Thread.currentThread().getContextClassLoader()).makeClass(classfile);
            CtMethod export = getCtMethod(ctClass, EXPORT_METHOD);
            if (export != null) {
                String setGrayGroup = String.format("super.setGroup(\"%s\");", grayGroup);
                String setGrayVersion = String.format("super.setVersion(\"%s\");", grayVersion);
                export.insertBefore(setGrayGroup + setGrayVersion);
            }

            return ctClass.toBytecode();
        } finally {
            if (ctClass != null) {
                ctClass.detach();
            }
        }
    }

    private static byte[] proxyEnhancer(InputStream classfile) throws Exception {

        CtClass ctClass = null;

        try {
            ctClass = ClassGenerator.getClassPool(Thread.currentThread().getContextClassLoader()).makeClass(classfile);
            CtMethod serRef = getCtMethod(ctClass, PROXY_KEY_METHOD);
            if (serRef != null) {
                String insertedCode =
                    String.format("ref=%s.getProxy(ref,this.getInterfaceClass());", ProxyGenerator.class.getName());
                serRef.insertBefore(insertedCode);
            }
            return ctClass.toBytecode();
        } finally {
            if (ctClass != null) {
                ctClass.detach();
            }
        }
    }

    private static byte[] referenceEnhancer(InputStream classfile) throws Exception {

        CtClass ctClass = null;

        try {
            ctClass = ClassGenerator.getClassPool(Thread.currentThread().getContextClassLoader()).makeClass(classfile);
            ctClass.getConstructors();
            CtMethod serRef = getCtMethod(ctClass, REFERENCE_METHOD);
            if (serRef != null) {
                String insertedCode = DubboTransformer.class.getName() + ".putReferee(this);";
                serRef.insertBefore(insertedCode);
            }
            return ctClass.toBytecode();
        } finally {
            if (ctClass != null) {
                ctClass.detach();
            }
        }
    }

    private static byte[] springBootClassLoaderEnhancer(InputStream classfile) throws Exception {

        CtClass ctClass = null;

        try {
            ctClass = ClassGenerator.getClassPool(Thread.currentThread().getContextClassLoader()).makeClass(classfile);
            CtConstructor[] constructors = ctClass.getConstructors();
            if (constructors != null && constructors.length > 0) {
                String insertedCode =
                    String.format("%s.setSpringBootClassLoader(this);", ProxyGenerator.class.getName());
                for (CtConstructor ctConstructor : constructors) {
                    ctConstructor.insertAfter(insertedCode);
                }
            }
            return ctClass.toBytecode();
        } finally {
            if (ctClass != null) {
                ctClass.detach();
            }
        }
    }

    private static CtMethod getCtMethod(final CtClass ctClass, String name) throws NotFoundException {
        CtClass searchCtClass = ctClass;
        while (searchCtClass != null) {
            try {
                return searchCtClass.getDeclaredMethod(name);
            } catch (NotFoundException ignored) {
            }
            searchCtClass = searchCtClass.getSuperclass();
        }
        throw new NotFoundException(name);
    }

    /**
     * 攫取当前应用的 Dubbo 消费者，用于获取公共信息
     *
     * 例如：应用信息、注册中心等
     *
     * 禁止删除
     */
    public static void putReferee(Object referee) {
        REFEREE_MAP.put(referee.getClass().getClassLoader(), referee);
    }

    /**
     * 灰度判断逻辑
     */
    @SuppressWarnings("unchecked")
    public static <T> T getGrayBean(final GrayContext grayContext, String method, Class<T> interfaceClass) {

        /*
         * 避免频繁重试
         */
        if (RETRY_LIMIT_MAP.getOrDefault(interfaceClass, Boolean.FALSE)) {
            return null;
        }

        /*
         * 配置优先级 method > service > global
         */
        Spy.Environment proxyEnvironment = Spy.getEnvironment();

        Map<String, String> grayRule = proxyEnvironment.getMap(ConstantKey.METHODS, Map.class)
            .get(interfaceClass.getName() + "#" + method);

        if (grayRule == null) {

            grayRule = proxyEnvironment.getMap(ConstantKey.SERVICES, Map.class).get(interfaceClass.getName());

            if (grayRule == null) {

                Map<String, String> globalGrayRule = proxyEnvironment.getObject(ConstantKey.GLOBAL, Map.class);

                if (globalGrayRule != null) {
                    grayRule = new HashMap<>(globalGrayRule);
                    grayRule.put(ConstantKey.Attributes.SERVICE, interfaceClass.getName());
                }
            }
        }

        T grayBean = null;
        if (grayRule != null) {
            try {
                String condition = grayRule.get(ConstantKey.Attributes.CONDITION);
                String grayGroup = grayRule.getOrDefault(ConstantKey.Attributes.GROUP, ConstantValue.GRAY_GROUP);
                String grayVersion = grayRule.getOrDefault(ConstantKey.Attributes.VERSION, ConstantValue.GRAY_VERSION);
                if (condition != null && condition.length() > 0) {
                    grayContext.put(ConstantKey.Variables.SERVICE, interfaceClass.getName());
                    grayContext.put(ConstantKey.Variables.METHOD, method);
                    if (ExpressUtils.getBoolean(condition, grayContext)) {
                        grayBean = reference(interfaceClass, grayGroup, grayVersion);
                    }
                } else {
                    grayBean = reference(interfaceClass, grayGroup, grayVersion);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return grayBean;
    }

    @SuppressWarnings("unchecked")
    private static <T> T reference(Class<T> service, String group, String version) {

        String referenceKey = service.getName() + "#" + group + "#" + version;

        Object reference = REFERENCE_MAP.get(referenceKey);
        if (reference != null) {
            return (T) reference;
        }

        try {
            boolean isApacheDubbo = Spy.getEnvironment().getObject(IS_APACHE_DUBBO, Boolean.class, Boolean.FALSE);
            Class<?> referenceConfig = service.getClassLoader().loadClass(
                isApacheDubbo ? "org.apache.dubbo.config.ReferenceConfig" : "com.alibaba.dubbo.config.ReferenceConfig"
            );
            final Object config = referenceConfig.newInstance();

            Object referee = REFEREE_MAP.get(service.getClassLoader());
            if (referee == null || !config.getClass().isAssignableFrom(referee.getClass())) {
                return null;
            }
            ReflectUtils.getMethodByName(config, "setApplication")
                .invoke(config, ReflectUtils.getFieldValueByName(referee, "application"));
            ReflectUtils.getMethodByName(config, "setRegistries")
                .invoke(config, ReflectUtils.getFieldValueByName(referee, "registries"));

            Object module = ReflectUtils.getFieldValueByName(referee, "module");
            if (module != null) {
                ReflectUtils.getMethodByName(config, "setModule", module.getClass())
                    .invoke(config, module);
            }

            Object monitor = ReflectUtils.getFieldValueByName(referee, "monitor");
            if (monitor != null) {
                ReflectUtils.getMethodByName(config, "setMonitor", monitor.getClass())
                    .invoke(config, monitor);
            }

            ReflectUtils.getMethodByName(config, "setInterface", Class.class)
                .invoke(config, service);
            ReflectUtils.getMethodByName(config, "setGroup", String.class)
                .invoke(config, group);
            ReflectUtils.getMethodByName(config, "setVersion", String.class)
                .invoke(config, version);
            ReflectUtils.getMethodByName(config, "setCheck", Boolean.class)
                .invoke(config, Boolean.FALSE);
            ReflectUtils.getMethodByName(config, "setRetries", Integer.class)
                .invoke(config, 0);

            //避免本地调用 injvm
            ReflectUtils.getMethodByName(config, "setScope", String.class)
                .invoke(config, "remote");

            final Method getCacheMethod = service.getClassLoader().loadClass(
                isApacheDubbo ? "org.apache.dubbo.config.utils.ReferenceConfigCache" :
                    "com.alibaba.dubbo.config.utils.ReferenceConfigCache")
                .getMethod("getCache");
            final Object referenceConfigCache = getCacheMethod.invoke(null);

            reference = ReflectUtils.getMethodByName(referenceConfigCache, "get", config.getClass())
                .invoke(referenceConfigCache, config);

            if (reference != null) {
                REFERENCE_MAP.put(referenceKey, reference);
            }

            return (T) reference;
        } catch (Exception e) {
            RETRY_LIMIT_MAP.put(service, PLACEHOLDER);
            throw new RuntimeException(e);
        }
    }

    public static class ProxyGenerator {

        private static final AtomicLong PROXY_CLASS_COUNTER = new AtomicLong(0);

        private static final String PACKAGE_NAME = ProxyGenerator.class.getPackage().getName();

        private static final Map<ClassLoader, Map<String, Object>> PROXY_CACHE_MAP = new WeakHashMap<>();

        private static final Object PENDING_GENERATION_MARKER = new Object();

        private static volatile ClassLoader springBootClassLoader;

        private ProxyGenerator() {
        }

        @SuppressWarnings({"unchecked"})
        public static <T> T getProxy(T bean, Class<T> service) {
            if (!service.isInterface()) {
                throw new RuntimeException(service + " is not a interface.");
            }

            String serviceName = service.getName();
            ClassLoader cl = bean.getClass().getClassLoader();
            try {
                if (Class.forName(serviceName, false, cl) != service) {
                    throw new IllegalArgumentException(service + " is not visible from class loader");
                }
            } catch (ClassNotFoundException ignored) {
            }

            Map<String, Object> cache;
            synchronized (PROXY_CACHE_MAP) {
                cache = PROXY_CACHE_MAP.computeIfAbsent(cl, k -> new HashMap<>(32));
            }

            T proxy = null;
            synchronized (cache) {
                do {
                    Object value = cache.get(serviceName);
                    if (value instanceof Reference<?>) {
                        proxy = (T) ((Reference<?>) value).get();
                        if (proxy != null) {
                            return proxy;
                        }
                    }

                    if (value == PENDING_GENERATION_MARKER) {
                        try {
                            cache.wait();
                        } catch (InterruptedException ignored) {
                        }
                    } else {
                        cache.put(serviceName, PENDING_GENERATION_MARKER);
                        break;
                    }
                }
                while (true);
            }

            String pkg = null;
            ClassGenerator ccp = null;
            try {
                ccp = ClassGenerator.newInstance(springBootClassLoader != null ? springBootClassLoader : cl);

                if (!Modifier.isPublic(service.getModifiers())) {
                    pkg = service.getPackage().getName();
                }

                ccp.addInterface(service);

                ccp.addField(String.format("public %s ref;", service.getName()));

                for (Method method : service.getMethods()) {
                    Class<?> rt = method.getReturnType();
                    Class<?>[] pts = method.getParameterTypes();

                    StringBuilder code = new StringBuilder();
                    code.append(ReflectUtils.modifier(method.getModifiers()))
                        .append(' ').append(ReflectUtils.getName(rt)).append(' ').append(method.getName());

                    code.append('(');
                    StringBuilder ctxInit = new StringBuilder();
                    for (int i = 0; i < pts.length; i++) {
                        if (i > 0) {
                            code.append(',');
                        }
                        code.append(ReflectUtils.getName(pts[i]));
                        code.append(" arg").append(i);
                        ctxInit.append(String.format("context.put(\"$%d\",($w)$%d);", i, i + 1));
                    }
                    code.append(')');
                    Class<?>[] ets = method.getExceptionTypes();
                    if (ets.length > 0) {
                        code.append(" throws ");
                        for (int i = 0; i < ets.length; i++) {
                            if (i > 0) {
                                code.append(',');
                            }
                            code.append(ReflectUtils.getName(ets[i]));
                        }
                    }
                    code.append('{');

                    code.append(GrayContext.class.getName()).append(" context=new ").append(GrayContext.class.getName()).append("();")
                        .append(ctxInit.toString())
                        .append(String.format("java.lang.String method = \"%s\";", method.getName()))
                        .append(service.getName()).append(" proxy=").append(DubboTransformer.class.getName()).append(".getGrayBean(")
                        .append("context,").append("method,").append(service.getTypeName()).append(".class);")
                        .append("return ($r)((proxy != null) ? proxy : ref).").append(method.getName()).append("($$);");

                    code.append("}");

                    ccp.addMethod(code.toString());
                }

                if (pkg == null) {
                    pkg = PACKAGE_NAME;
                }

                String proxyName = pkg + ".proxy$generator" + PROXY_CLASS_COUNTER.getAndIncrement();
                ccp.setClassName(proxyName);
                ccp.addDefaultConstructor();
                Class<T> clazz = (Class<T>) ccp.toClass();
                proxy = clazz.newInstance();
                proxy.getClass().getField("ref").set(proxy, bean);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            } finally {
                if (ccp != null) {
                    ccp.release();
                }
                synchronized (cache) {
                    if (proxy == null) {
                        cache.remove(serviceName);
                    } else {
                        cache.put(serviceName, new WeakReference<>(proxy));
                    }
                    cache.notifyAll();
                }
            }
            return proxy;
        }

        /**
         * 获取 SpringBoot ClassLoader
         *
         * 禁止删除
         */
        public static void setSpringBootClassLoader(final Object classLoader) {
            if (springBootClassLoader == null && classLoader instanceof ClassLoader) {
                springBootClassLoader = (ClassLoader) classLoader;
            }
        }
    }
}
