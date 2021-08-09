package org.mytang.fairydust.core.service.dubbo;

import org.mytang.fairydust.core.FairyDustEnvironment;
import org.junit.Test;

public class DubboTransformerTest {

    @Test
    public void testCase() {
        FairyDustEnvironment fairyDustEnvironment = new FairyDustEnvironment();
        fairyDustEnvironment.put(ConstantKey.GLOBAL, "{\"condition\":\"true\"}");
        fairyDustEnvironment.put(ConstantKey.SERVICES, "[{\"service\":\"service1\",\"condition\":\"true\"},{\"service\":\"service2\",\"condition\":\"true\"}]");
        fairyDustEnvironment.put(ConstantKey.METHODS, "[{\"service\":\"service1\",\"method\":\"method11\",\"condition\":\"true\"},{\"service\":\"service2\",\"method\":\"method21\",\"condition\":\"true\"}]");
        new DubboTransformer(fairyDustEnvironment);
    }
}
