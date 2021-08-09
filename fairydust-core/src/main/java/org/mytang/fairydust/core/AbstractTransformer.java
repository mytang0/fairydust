package org.mytang.fairydust.core;

import java.lang.instrument.ClassFileTransformer;

import static java.util.Objects.requireNonNull;

/**
 * @author tangmengyang
 */
public abstract class AbstractTransformer implements ClassFileTransformer {

    protected FairyDustEnvironment fairyDustEnvironment;

    public AbstractTransformer(FairyDustEnvironment fairyDustEnvironment) {
        environmentResolve(this.fairyDustEnvironment =
            requireNonNull(fairyDustEnvironment, "fairyDustEnvironment 不能为空"));
    }

    /**
     * 代理环境处理
     */
    public void environmentResolve(FairyDustEnvironment environment) {
    }
}
