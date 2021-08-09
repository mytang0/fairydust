package org.mytang.fairydust.core;

import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

public class ProxyEnvironmentTest {

    @Test
    public void testCase() {
        FairyDustEnvironment fairyDustEnvironment = new FairyDustEnvironment();

        Rule rule = new Rule();
        fairyDustEnvironment.put("rule", rule);

        fairyDustEnvironment.put("name", "java");

        Assert.assertEquals(rule, fairyDustEnvironment.getObject("rule", Rule.class));

        Assert.assertNull(fairyDustEnvironment.getObject("rule", String.class));

        Assert.assertNotNull(fairyDustEnvironment.getMap("undefined", Rule.class));

        Map<String, Rule> ruleMap = new HashMap<>();
        ruleMap.put("only", new Rule());
        fairyDustEnvironment.put("rules", ruleMap);

        Assert.assertNotNull(fairyDustEnvironment.getMap("rules", String.class));
    }

    private static class Rule {
    }
}
