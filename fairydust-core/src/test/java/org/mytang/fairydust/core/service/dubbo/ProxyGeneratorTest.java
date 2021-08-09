package org.mytang.fairydust.core.service.dubbo;

import org.mytang.fairydust.core.ProxyEnvironmentTest;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProxyGeneratorTest {

    private static final Logger log = LoggerFactory.getLogger(ProxyEnvironmentTest.class);

    @Test
    public void testCase() {
        TestService instance =
            DubboTransformer.ProxyGenerator.getProxy(new TestServiceImpl(), TestService.class);

        instance.age(99);

        instance.hello("java");

        instance.bye("java");

        log.info("hello world");
    }

    private static class TestServiceImpl implements TestService {

        @Override
        public int age(int age) {
            System.out.println(age);
            return age;
        }

        @Override
        public void hello(String name) {
            System.out.println(name);
        }

        @Override
        public String bye(String name) {
            System.out.println(name);
            return null;
        }
    }

    interface TestService {

        int age(int age);

        void hello(String name);

        String bye(String name);
    }
}
