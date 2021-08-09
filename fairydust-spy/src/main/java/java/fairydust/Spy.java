package java.fairydust;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author tangmengyang
 */
public class Spy {

    public static final AbstractSpy NOP_SPY = new NopSpy();

    private static volatile AbstractSpy spyInstance = NOP_SPY;

    private static final Environment ENVIRONMENT = new Environment();

    public static volatile boolean INITED;

    public static AbstractSpy getSpy() {
        return spyInstance;
    }

    public static void setSpy(AbstractSpy spy) {
        spyInstance = spy;
    }

    public static void setNopSpy() {
        setSpy(NOP_SPY);
    }

    public static Environment getEnvironment() {
        return ENVIRONMENT;
    }

    public static boolean isNopSpy() {
        return NOP_SPY == spyInstance;
    }

    public static void init() {
        INITED = true;
    }

    public static boolean isInited() {
        return INITED;
    }

    public static void destroy() {
        setNopSpy();
        INITED = false;
    }

    public static abstract class AbstractSpy {
    }

    static class NopSpy extends AbstractSpy {
    }

    public static class Environment {

        private final Map<String, Object> properties = new ConcurrentHashMap<>();

        public Map<String, Object> getProperties() {
            return properties;
        }

        public void putAll(Map<String, Object> properties) {
            if (properties != null && this.properties != properties) {
                this.properties.putAll(properties);
            }
        }

        public void put(String property, Object value) {
            properties.put(property, value);
        }

        public Object getObject(final String property) {
            return properties.get(property);
        }

        public void remove(final String property) {
            properties.remove(property);
        }

        public String getString(final String property) {
            Object value = properties.get(property);
            if (null == value) {
                return null;
            }

            return (value instanceof String) ?
                (String) value :
                String.valueOf(value);
        }

        public String getString(final String property, final String defaultValue) {
            String value = this.getString(property);
            if (null == value) {
                return defaultValue;
            }
            return value;
        }

        public Integer getInteger(final String property) {
            Object value = properties.get(property);
            if (null == value) {
                return null;
            }

            try {
                return Integer.valueOf(String.valueOf(value));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public Integer getInteger(final String property, int defaultValue) {
            Integer value = this.getInteger(property);
            if (null == value) {
                return defaultValue;
            }
            return value;
        }

        public Long getLong(final String property) {
            Object value = properties.get(property);
            if (null == value) {
                return null;
            }

            try {
                return Long.valueOf(String.valueOf(value));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public Long getLong(final String property, long defaultValue) {
            Long value = this.getLong(property);
            if (null == value) {
                return defaultValue;
            }
            return value;
        }

        public Double getDouble(final String property) {
            Object value = properties.get(property);
            if (null == value) {
                return null;
            }

            try {
                return value instanceof Double ?
                    (Double) value :
                    Double.valueOf(String.valueOf(value));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public Double getDouble(final String property, final Double defaultValue) {
            Object value = properties.get(property);
            if (null == value) {
                return defaultValue;
            }

            try {
                return value instanceof Double ?
                    (Double) value :
                    Double.valueOf(String.valueOf(value));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @SuppressWarnings("unchecked")
        public <V> Map<String, V> getMap(final String property, Class<V> v) {
            assert v != null;
            Object value = properties.get(property);
            if (value instanceof Map && !((Map) value).isEmpty()) {
                return (Map<String, V>) value;
            }
            return new HashMap<>(4);
        }

        @SuppressWarnings("unchecked")
        public <V> V getObject(final String property, Class<V> v) {
            assert v != null;
            Object value = properties.get(property);
            if (value != null) {
                return (V) value;
            }
            return null;
        }

        @SuppressWarnings("unchecked")
        public <V> V getObject(final String property, Class<V> v, final V defaultValue) {
            assert v != null;
            Object value = properties.get(property);
            if (value != null) {
                return (V) value;
            }
            return defaultValue;
        }
    }
}
