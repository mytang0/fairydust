package org.mytang.fairydust.core.service;

import com.ql.util.express.IExpressContext;
import java.util.HashMap;
import java.util.Map;

/**
 * @author tangmengyang
 */
public class GrayContext implements IExpressContext<String, Object> {

    private final Map<String, Object> context = new HashMap<>();

    @Override
    public Object get(Object key) {
        return (key instanceof String) ?
            context.get(key) :
            context.get(String.valueOf(key));
    }

    @Override
    public Object put(String name, Object object) {
        context.put(name, object);
        return null;
    }
}
