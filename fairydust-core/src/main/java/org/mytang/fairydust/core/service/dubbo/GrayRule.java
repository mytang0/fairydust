package org.mytang.fairydust.core.service.dubbo;

import java.io.Serializable;

/**
 * @author tangmengyang
 */
public class GrayRule implements Serializable {
    private static final long serialVersionUID = -4073106385458550312L;

    /**
     * 灰度 dubbo 服务分组
     */
    static final String GRAY_GROUP = "gray";

    /**
     * 灰度 dubbo 服务版本
     */
    static final String GRAY_VERSION = "9.9.9";

    /**
     * 灰度服务
     */
    private String service;

    /**
     * 灰度方法名
     */
    private String method;

    /**
     * 灰度条件
     *
     * <pre>
     * #see https://github.com/alibaba/QLExpress
     * </pre>
     */
    private String condition;

    /**
     * 服务分组
     */
    private String group = GRAY_GROUP;

    /**
     * 服务版本
     */
    private String version = GRAY_VERSION;

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}