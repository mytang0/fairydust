package org.mytang.fairydust.core.service.dubbo;

/**
 * @author tangmengyang
 */
public final class ConstantKey {
    public static final String GRAY_GROUP = "fairydust.gray.group";
    public static final String GRAY_VERSION = "fairydust.gray.version";

    /**
     * 灰度规则
     */
    public static final String GLOBAL = "fairydust.gray.rule.global";
    public static final String SERVICES = "fairydust.gray.rule.services";
    public static final String METHODS = "fairydust.gray.rule.methods";

    /**
     * 规则属性
     */
    public final class Attributes {
        /**
         * 灰度服务
         */
        static final String SERVICE = "service";

        /**
         * 灰度方法名
         */
        public static final String METHOD = "method";

        /**
         * 灰度条件
         *
         * <pre>
         * #see https://github.com/alibaba/QLExpress
         * </pre>
         */
        public static final String CONDITION = "condition";

        /**
         * 服务分组
         *
         * 默认值为
         *
         * @see ConstantValue#GRAY_GROUP
         */
        public static final String GROUP = "group";

        /**
         * 服务版本
         *
         * 默认值为
         *
         * @see ConstantValue#GRAY_VERSION
         */
        public static final String VERSION = "version";
    }

    /**
     * 变量
     */
    public final class Variables {
        /**
         * 当前服务名
         */
        static final String SERVICE = "service";

        /**
         * 当前方法名
         */
        public static final String METHOD = "method";
    }
}
