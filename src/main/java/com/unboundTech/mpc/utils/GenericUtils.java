package com.unboundTech.mpc.utils;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GenericUtils {

    private static final Map<Class<?>, Class<?>> cachedType = new ConcurrentHashMap<>();

    public static Class<?> getSuperClassGenericType(Class<?> clazz) {
        Class<?> type = cachedType.get(clazz);
        if (type == null) {
            type = getSuperClassGenericType(clazz, 0);
            cachedType.put(clazz, type);
        }
        return type;
    }

    public static Class<?> getSuperClassGenericType(Class<?> clazz, int index) {
        Type superClass = clazz.getGenericSuperclass();

        if (!(superClass instanceof ParameterizedType)) {
            return Object.class;
        }

        Type[] params = ((ParameterizedType) superClass).getActualTypeArguments();

        if (index >= params.length || index < 0) {
            return Object.class;
        }

        if (!(params[index] instanceof Class)) {
            return Object.class;
        }

        return (Class<?>) params[index];
    }



}
