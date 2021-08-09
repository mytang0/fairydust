package org.mytang.fairydust.core.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author tangmengyang
 */
public final class ReflectUtils {
    /**
     * void(V).
     */
    public static final char JVM_VOID = 'V';

    /**
     * boolean(Z).
     */
    public static final char JVM_BOOLEAN = 'Z';

    /**
     * byte(B).
     */
    public static final char JVM_BYTE = 'B';

    /**
     * char(C).
     */
    public static final char JVM_CHAR = 'C';

    /**
     * double(D).
     */
    public static final char JVM_DOUBLE = 'D';

    /**
     * float(F).
     */
    public static final char JVM_FLOAT = 'F';

    /**
     * int(I).
     */
    public static final char JVM_INT = 'I';

    /**
     * long(J).
     */
    public static final char JVM_LONG = 'J';

    /**
     * short(S).
     */
    public static final char JVM_SHORT = 'S';

    private ReflectUtils() {
    }

    public static String modifier(int mod) {
        if (Modifier.isPublic(mod)) {
            return "public";
        }
        if (Modifier.isProtected(mod)) {
            return "protected";
        }
        if (Modifier.isPrivate(mod)) {
            return "private";
        }
        return "";
    }

    public static String getName(Class<?> c) {
        if (c.isArray()) {
            StringBuilder sb = new StringBuilder();
            do {
                sb.append("[]");
                c = c.getComponentType();
            }
            while (c.isArray());

            return c.getName() + sb.toString();
        }
        return c.getName();
    }

    /**
     * get method desc. "(I)I", "()V", "(Ljava/lang/String;Z)V"
     *
     * @param m method.
     * @return desc.
     */
    public static String getDescWithoutMethodName(Method m) {
        StringBuilder ret = new StringBuilder();
        ret.append('(');
        Class<?>[] parameterTypes = m.getParameterTypes();
        for (int i = 0; i < parameterTypes.length; i++) {
            ret.append(getDesc(parameterTypes[i]));
        }
        ret.append(')').append(getDesc(m.getReturnType()));
        return ret.toString();
    }

    /**
     * get constructor desc. "()V", "(Ljava/lang/String;I)V"
     *
     * @param c constructor.
     * @return desc
     */
    public static String getDesc(final Constructor<?> c) {
        StringBuilder ret = new StringBuilder("(");
        Class<?>[] parameterTypes = c.getParameterTypes();
        for (int i = 0; i < parameterTypes.length; i++) {
            ret.append(getDesc(parameterTypes[i]));
        }
        ret.append(')').append('V');
        return ret.toString();
    }

    /**
     * get class desc. boolean[].class => "[Z" Object.class => "Ljava/lang/Object;"
     *
     * @param c class.
     * @return desc.
     */
    public static String getDesc(Class<?> c) {
        StringBuilder ret = new StringBuilder();

        while (c.isArray()) {
            ret.append('[');
            c = c.getComponentType();
        }

        if (c.isPrimitive()) {
            String t = c.getName();
            if ("void".equals(t)) {
                ret.append(JVM_VOID);
            } else if ("boolean".equals(t)) {
                ret.append(JVM_BOOLEAN);
            } else if ("byte".equals(t)) {
                ret.append(JVM_BYTE);
            } else if ("char".equals(t)) {
                ret.append(JVM_CHAR);
            } else if ("double".equals(t)) {
                ret.append(JVM_DOUBLE);
            } else if ("float".equals(t)) {
                ret.append(JVM_FLOAT);
            } else if ("int".equals(t)) {
                ret.append(JVM_INT);
            } else if ("long".equals(t)) {
                ret.append(JVM_LONG);
            } else if ("short".equals(t)) {
                ret.append(JVM_SHORT);
            }
        } else {
            ret.append('L');
            ret.append(c.getName().replace('.', '/'));
            ret.append(';');
        }
        return ret.toString();
    }

    public static Object getFieldValueByName(Object obj, String fieldName)
        throws IllegalArgumentException, IllegalAccessException {
        Field field = getFieldByName(obj, fieldName);
        if (field == null) {
            return null;
        }
        if (!field.isAccessible()) {
            field.setAccessible(true);
        }
        return field.get(obj);
    }

    public static void setValueByFieldName(Object obj, String fieldName, Object value)
        throws IllegalArgumentException, IllegalAccessException {
        Field field = getFieldByName(obj, fieldName);
        if (field == null) {
            return;
        }
        if (!field.isAccessible()) {
            field.setAccessible(true);
        }
        field.set(obj, value);
    }

    public static Field getFieldByName(Object obj, String fieldName) {
        Class superClass = obj.getClass();

        while (superClass != Object.class) {
            try {
                return superClass.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                superClass = superClass.getSuperclass();
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static Method getMethodByName(Object obj, String methodName, Class<?>... parameterTypes)
        throws IllegalArgumentException, NoSuchMethodException {
        Class superClass = obj.getClass();

        while (superClass != Object.class) {
            try {
                if (parameterTypes == null || parameterTypes.length == 0) {
                    return getMethodByName(superClass, methodName);
                } else {
                    return superClass.getMethod(methodName, parameterTypes);
                }
            } catch (NoSuchMethodException | SecurityException e) {
                if (parameterTypes != null && parameterTypes.length > 0) {
                    try {
                        return getMethodByNameAndParameterTypes(superClass, methodName, parameterTypes);
                    } catch (NoSuchMethodException | SecurityException ignored) {
                    }
                }
                superClass = superClass.getSuperclass();
            }
        }
        throw new NoSuchMethodException(
            String.format("%s#%s", obj.getClass().getName(), methodName)
        );
    }

    private static Method getMethodByName(Class<?> clazz, String methodName)
        throws NoSuchMethodException {
        Method[] methods = clazz.getMethods();
        for (Method method : methods) {
            if (method.getName().equals(methodName)) {
                return method;
            }
        }
        throw new NoSuchMethodException(
            String.format("%s#%s", clazz.getName(), methodName)
        );
    }

    private static Method getMethodByNameAndParameterTypes(Class<?> clazz,
        String methodName, Class<?>[] parameterTypes)
        throws NoSuchMethodException {

        Method[] methods = clazz.getMethods();
        for (Method method : methods) {
            if (method.getName().equals(methodName)) {
                if (parameterTypes.length == method.getParameterCount()) {
                    boolean match = true;
                    Class<?>[] actualParameterTypes = method.getParameterTypes();

                    for (int idx = 0; idx < actualParameterTypes.length; idx ++) {
                        if (!actualParameterTypes[idx].isAssignableFrom(parameterTypes[idx])) {
                            match = false;
                            break;
                        }
                    }

                    if (match) {
                        return method;
                    }
                }
            }
        }
        throw new NoSuchMethodException(
            String.format("%s#%s", clazz.getName(), methodName)
        );
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> declaredFieldMap(Object object, int modifierFilter) {
        if (object instanceof Map) {
            try {
                //运行阶段泛型已经擦除，只要是 Map，都会成功返回，使用时请注意
                return (Map<String, Object>) object;
            } catch (Exception e) {
                //
            }
        }

        Map<String, Object> map = new HashMap<>(32);
        if (object == null) {
            return map;
        }

        Set<Field> fieldSet = getAllFields(object.getClass());
        Iterator<Field> iterator = fieldSet.iterator();

        for (; iterator.hasNext(); ) {
            Field f = iterator.next();
            try {
                if (modifierFilter != 0 &&
                    (modifierFilter & f.getModifiers()) != 0) {
                    continue;
                }
                f.setAccessible(true);
                Object field = f.get(object);
                map.put(f.getName(), field);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
        return map;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> declaredFieldMap(Object object, boolean emptyStringFilter) {
        if (object instanceof Map) {
            try {
                //运行阶段泛型已经擦除，只要是 Map，都会成功返回，使用时请注意
                return (Map<String, Object>) object;
            } catch (Exception e) {
                //
            }
        }

        Map<String, Object> map = new HashMap<>(32);
        if (object == null) {
            return map;
        }

        Set<Field> fieldSet = getAllFields(object.getClass());
        Iterator<Field> iterator = fieldSet.iterator();

        for (; iterator.hasNext(); ) {
            Field f = iterator.next();
            try {
                f.setAccessible(true);
                Object field = f.get(object);
                //空字符串过滤
                if (emptyStringFilter &&
                    f.getType() == String.class &&
                    (field == null || ((String) field).length() == 0)) {
                    continue;
                }
                map.put(f.getName(), field);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
        return map;
    }

    /**
     * 当同名属性类型不一致时，JVM 会抛异常
     * <p>
     * 缺陷：不同ClassLoader加载的同一个类对 JVM 来说是不同的
     *
     * @param src 源对象
     * @param dest 目标对象
     */
    @SuppressWarnings("unchecked")
    public static <T> T set(Object src, T dest, boolean emptyStringFilter) {
        if (src == null) {
            return dest;
        }

        if (dest == null) {
            return null;
        }

        Map<String, Object> srcMap = declaredFieldMap(src, emptyStringFilter);

        if (dest instanceof Map) {
            //运行阶段泛型已经擦除，只要是 Map，都会成功返回，使用时请注意
            ((Map) dest).putAll(srcMap);
            return dest;
        }

        Object object;
        Set<Field> fieldSet = getAllFields(dest.getClass());
        Iterator<Field> iterator = fieldSet.iterator();
        for (; iterator.hasNext(); ) {
            Field f = iterator.next();
            f.setAccessible(true);
            //不用修改常量
            if (Modifier.isFinal(f.getModifiers()) ||
                Modifier.isStatic(f.getModifiers()) ||
                (object = srcMap.get(f.getName())) == null) {
                continue;
            }

            if (!isAssignable(object.getClass(), f.getType())) {
                continue;
            }

            try {
                f.set(dest, object);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
        return dest;
    }

    public static Set<Field> getAllFields(final Class<?> type) {
        Set<Field> result = new HashSet<>(32);
        for (Class<?> t : getAllSuperTypes(type)) {
            result.addAll(getFields(t));
        }
        return result;
    }

    public static Set<Class<?>> getAllSuperTypes(final Class<?> type) {
        Set<Class<?>> result = new LinkedHashSet<>();
        if (type != null && !type.equals(Object.class)) {
            result.add(type);
            for (Class<?> supertype : getSuperTypes(type)) {
                result.addAll(getAllSuperTypes(supertype));
            }
        }
        return result;
    }

    public static Set<Class<?>> getSuperTypes(Class<?> type) {
        Set<Class<?>> result = new LinkedHashSet<>();
        Class<?> superclass = type.getSuperclass();
        Class<?>[] interfaces = type.getInterfaces();
        if (superclass != null && !superclass.equals(Object.class)) {
            result.add(superclass);
        }
        if (interfaces != null && interfaces.length > 0) {
            result.addAll(Arrays.asList(interfaces));
        }
        return result;
    }

    public static Set<Field> getFields(Class<?> type) {
        Field[] fields = type.getDeclaredFields();
        Set<Field> set = new HashSet<>(fields.length);
        set.addAll(Arrays.asList(fields));
        return set;
    }

    public static Map<String, Object> declaredFieldMap(Object object) {
        return declaredFieldMap(object, false);
    }

    public static <T> T set(Object src, T dest) {
        return set(src, dest, false);
    }


    /*------------------------------- come from org.apache.commons.lang3 ---------------------------------*/

    /**
     * Maps primitive {@code Class}es to their corresponding wrapper {@code Class}.
     */
    private static final Map<Class<?>, Class<?>> primitiveWrapperMap = new HashMap<Class<?>, Class<?>>();

    static {
        primitiveWrapperMap.put(Boolean.TYPE, Boolean.class);
        primitiveWrapperMap.put(Byte.TYPE, Byte.class);
        primitiveWrapperMap.put(Character.TYPE, Character.class);
        primitiveWrapperMap.put(Short.TYPE, Short.class);
        primitiveWrapperMap.put(Integer.TYPE, Integer.class);
        primitiveWrapperMap.put(Long.TYPE, Long.class);
        primitiveWrapperMap.put(Double.TYPE, Double.class);
        primitiveWrapperMap.put(Float.TYPE, Float.class);
        primitiveWrapperMap.put(Void.TYPE, Void.TYPE);
    }

    /**
     * Maps wrapper {@code Class}es to their corresponding primitive types.
     */
    private static final Map<Class<?>, Class<?>> wrapperPrimitiveMap = new HashMap<Class<?>, Class<?>>();

    static {
        for (final Map.Entry<Class<?>, Class<?>> entry : primitiveWrapperMap.entrySet()) {
            final Class<?> primitiveClass = entry.getKey();
            final Class<?> wrapperClass = entry.getValue();
            if (!primitiveClass.equals(wrapperClass)) {
                wrapperPrimitiveMap.put(wrapperClass, primitiveClass);
            }
        }
    }

    /**
     * <p>Checks if one {@code Class} can be assigned to a variable of
     * another {@code Class}.</p>
     *
     * <p>Unlike the {@link Class#isAssignableFrom(Class)} method,
     * this method takes into account widenings of primitive classes and {@code null}s.</p>
     *
     * <p>Primitive widenings allow an int to be assigned to a long, float or
     * double. This method returns the correct result for these cases.</p>
     *
     * <p>{@code Null} may be assigned to any reference type. This method
     * will return {@code true} if {@code null} is passed in and the toClass is non-primitive.</p>
     *
     * <p>Specifically, this method tests whether the type represented by the
     * specified {@code Class} parameter can be converted to the type represented by this {@code Class} object via an
     * identity conversion widening primitive or widening reference conversion. See
     * <em><a href="http://docs.oracle.com/javase/specs/">The Java Language Specification</a></em>,
     * sections 5.1.1, 5.1.2 and 5.1.4 for details.</p>
     *
     * @param cls the Class to check, may be null
     * @param toClass the Class to try to assign into, returns false if null
     * @return {@code true} if assignment possible
     */
    public static boolean isAssignable(Class<?> cls, final Class<?> toClass) {
        if (toClass == null) {
            return false;
        }
        // have to check for null, as isAssignableFrom doesn't
        if (cls == null) {
            return !toClass.isPrimitive();
        }
        //autoboxing:
        if (cls.isPrimitive() && !toClass.isPrimitive()) {
            cls = primitiveToWrapper(cls);
            if (cls == null) {
                return false;
            }
        }
        if (toClass.isPrimitive() && !cls.isPrimitive()) {
            cls = wrapperToPrimitive(cls);
            if (cls == null) {
                return false;
            }
        }

        if (cls.equals(toClass)) {
            return true;
        }
        if (cls.isPrimitive()) {
            if (!toClass.isPrimitive()) {
                return false;
            }
            if (Integer.TYPE.equals(cls)) {
                return Long.TYPE.equals(toClass)
                    || Float.TYPE.equals(toClass)
                    || Double.TYPE.equals(toClass);
            }
            if (Long.TYPE.equals(cls)) {
                return Float.TYPE.equals(toClass)
                    || Double.TYPE.equals(toClass);
            }
            if (Boolean.TYPE.equals(cls)) {
                return false;
            }
            if (Double.TYPE.equals(cls)) {
                return false;
            }
            if (Float.TYPE.equals(cls)) {
                return Double.TYPE.equals(toClass);
            }
            if (Character.TYPE.equals(cls)) {
                return Integer.TYPE.equals(toClass)
                    || Long.TYPE.equals(toClass)
                    || Float.TYPE.equals(toClass)
                    || Double.TYPE.equals(toClass);
            }
            if (Short.TYPE.equals(cls)) {
                return Integer.TYPE.equals(toClass)
                    || Long.TYPE.equals(toClass)
                    || Float.TYPE.equals(toClass)
                    || Double.TYPE.equals(toClass);
            }
            if (Byte.TYPE.equals(cls)) {
                return Short.TYPE.equals(toClass)
                    || Integer.TYPE.equals(toClass)
                    || Long.TYPE.equals(toClass)
                    || Float.TYPE.equals(toClass)
                    || Double.TYPE.equals(toClass);
            }
            // should never get here
            return false;
        }
        return toClass.isAssignableFrom(cls);
    }

    /**
     * <p>Converts the specified primitive Class object to its corresponding
     * wrapper Class object.</p>
     *
     * <p>NOTE: From v2.2, this method handles {@code Void.TYPE},
     * returning {@code Void.TYPE}.</p>
     *
     * @param cls the class to convert, may be null
     * @return the wrapper class for {@code cls} or {@code cls} if {@code cls} is not a primitive. {@code null} if null
     * input.
     * @since 2.1
     */
    public static Class<?> primitiveToWrapper(final Class<?> cls) {
        Class<?> convertedClass = cls;
        if (cls != null && cls.isPrimitive()) {
            convertedClass = primitiveWrapperMap.get(cls);
        }
        return convertedClass;
    }

    /**
     * <p>Converts the specified wrapper class to its corresponding primitive
     * class.</p>
     *
     * <p>This method is the counter part of {@code primitiveToWrapper()}.
     * If the passed in class is a wrapper class for a primitive type, this primitive type will be returned (e.g. {@code
     * Integer.TYPE} for {@code Integer.class}). For other classes, or if the parameter is
     * <b>null</b>, the return value is <b>null</b>.</p>
     *
     * @param cls the class to convert, may be <b>null</b>
     * @return the corresponding primitive type if {@code cls} is a wrapper class, <b>null</b> otherwise
     * @see #primitiveToWrapper(Class)
     * @since 2.4
     */
    public static Class<?> wrapperToPrimitive(final Class<?> cls) {
        return wrapperPrimitiveMap.get(cls);
    }
}