package org.mytang.fairydust.core.util;

import com.ql.util.express.IExpressContext;
import org.junit.Assert;
import org.junit.Test;

public class ExpressUtilsTest {

    @Test
    public void testCase() {
        Assert.assertTrue(
            ExpressUtils.getBoolean("true", new IExpressContext<String, Object>() {

                @Override
                public Object get(Object key) {
                    return null;
                }

                @Override
                public Object put(String name, Object object) {
                    return null;
                }
            })
        );
    }
}
