/*
 * Copyright (C) 2022-2022 Huawei Technologies Co., Ltd. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.sermant.core.utils;

import io.sermant.core.classloader.ClassLoaderManager;
import io.sermant.core.common.LoggerFactory;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ReflectUtils
 *
 * @author zhouss
 * @since 2022-05-20
 */
public class ReflectUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger();

    /**
     * Filed cache
     */
    private static final Map<Class<?>, Map<String, Field>> FIELD_CACHE = new ConcurrentHashMap<>();

    /**
     * Method cache, key: class#method(params) value: method
     */
    private static final Map<String, Method> METHOD_CACHE = new ConcurrentHashMap<>();

    private static final Map<String, Optional<Class<?>>> CLASS_CACHE = new ConcurrentHashMap<>();

    /**
     * Constructor cache
     */
    private static final Map<String, Optional<Constructor<?>>> CONSTRUCTOR_CACHE = new ConcurrentHashMap<>();

    /**
     * The initial capacity of a field cached for a single class
     */
    private static final int INIT_CLASS_FILED_CACHE_SIZE = 4;

    private static final int EXTRA_LENGTH_FOR_METHOD_KEY = 3;

    private ReflectUtils() {
    }

    /**
     * invoke parameterless method via reflection
     *
     * @param target Target method
     * @param methodName Method name
     * @return 结果
     */
    public static Optional<Object> invokeMethodWithNoneParameter(Object target, String methodName) {
        return invokeMethod(target, methodName, null, null);
    }

    /**
     * invoke method via reflection
     *
     * @param target Target method
     * @param methodName Method name
     * @param paramsType Parameter type
     * @param params parameter
     * @return result
     */
    public static Optional<Object> invokeMethod(Object target, String methodName, Class<?>[] paramsType,
            Object[] params) {
        if (methodName == null || target == null) {
            return Optional.empty();
        }
        final Optional<Method> method = findMethod(target.getClass(), methodName, paramsType);
        if (method.isPresent()) {
            return invokeMethod(target, method.get(), params);
        }
        return Optional.empty();
    }

    /**
     * invoke static methods
     *
     * @param className Class fully qualified name
     * @param methodName Method name
     * @param paramsType Parameter type
     * @param params parameter
     * @return invoke result
     */
    public static Optional<Object> invokeMethod(String className, String methodName, Class<?>[] paramsType,
            Object[] params) {
        final Optional<Class<?>> clazz = loadClass(className);
        if (!clazz.isPresent()) {
            return Optional.empty();
        }
        return invokeMethod(clazz.get(), methodName, paramsType, params);
    }

    /**
     * invoke static methods
     *
     * @param clazz Class object
     * @param methodName Method name
     * @param paramsType Parameter type
     * @param params parameter
     * @return invoke result
     */
    public static Optional<Object> invokeMethod(Class<?> clazz, String methodName, Class<?>[] paramsType,
            Object[] params) {
        final Optional<Method> method = findMethod(clazz, methodName, paramsType);
        if (method.isPresent()) {
            return invokeMethod(null, method.get(), params);
        }
        return Optional.empty();
    }

    /**
     * invoke method
     *
     * @param method method
     * @param target Target object
     * @param params Method parameter
     * @return result
     */
    public static Optional<Object> invokeMethod(Object target, Method method, Object[] params) {
        try {
            if (params == null) {
                return Optional.ofNullable(method.invoke(target));
            }
            return Optional.ofNullable(method.invoke(target, params));
        } catch (InvocationTargetException | IllegalAccessException ex) {
            LOGGER.warning(String.format(Locale.ENGLISH, "Can not invoke method [%s] in class [%s], reason: %s",
                    method.getName(), target == null ? "static method " : target.getClass().getName(),
                    ex.getMessage()));
        }
        return Optional.empty();
    }

    private static Optional<Class<?>> loadClass(String className) {
        if (className == null) {
            return Optional.empty();
        }
        return CLASS_CACHE.computeIfAbsent(className, value -> {
            final ClassLoader contextClassLoader = ClassLoaderManager.getContextClassLoaderOrUserClassLoader();
            try {
                return Optional.ofNullable(contextClassLoader.loadClass(className));
            } catch (ClassNotFoundException ignored) {
                // directly return when no class found
                return Optional.empty();
            }
        });
    }

    /**
     * Find method, if the subclass cannot be found, then iterate to find the parent class
     *
     * @param clazz class
     * @param methodName Method name
     * @param paramsType Method parameter
     * @return method
     */
    public static Optional<Method> findMethod(Class<?> clazz, String methodName, Class<?>[] paramsType) {
        if (clazz == null) {
            return Optional.empty();
        }
        final String methodKey = buildMethodKey(clazz, methodName, paramsType);
        try {
            Method method = METHOD_CACHE.get(methodKey);
            if (method != null) {
                return Optional.of(method);
            }
            method = setObjectAccessible(clazz.getDeclaredMethod(methodName, paramsType));
            METHOD_CACHE.put(methodKey, method);
            return Optional.of(method);
        } catch (NoSuchMethodException ex) {
            Optional<Method> method = findSuperClass(clazz, methodName, paramsType);
            if (method.isPresent()) {
                return method;
            }
        }
        return Optional.empty();
    }

    private static Optional<Method> findSuperClass(Class<?> clazz, String methodName, Class<?>[] paramsType) {
        if (clazz.getSuperclass() != null || clazz.getInterfaces().length > 0) {
            Optional<Method> method = findMethod(clazz.getSuperclass(), methodName, paramsType);
            if (method.isPresent()) {
                return method;
            }
            for (Class<?> interfaceClass : clazz.getInterfaces()) {
                method = findMethod(interfaceClass, methodName, paramsType);
                if (method.isPresent()) {
                    return method;
                }
            }
        } else {
            LOGGER.warning(String.format(Locale.ENGLISH, "Can not find method named [%s] from class [%s]",
                    methodName, clazz.getName()));
        }
        return Optional.empty();
    }

    /**
     * Build with constructor
     *
     * @param className Class fully qualified name
     * @param paramsTypes Constructor parameter types
     * @param params Constructor parameter
     * @return instance object
     */
    public static Optional<Object> buildWithConstructor(String className, Class<?>[] paramsTypes, Object[] params) {
        final Class<?> clazz = loadClass(className).orElse(null);
        return buildWithConstructor(clazz, paramsTypes, params);
    }

    /**
     * Build with constructor
     *
     * @param clazz class
     * @param paramsTypes Constructor parameter types
     * @param params Constructor parameter
     * @return instance object
     */
    public static Optional<Object> buildWithConstructor(Class<?> clazz, Class<?>[] paramsTypes, Object[] params) {
        if (clazz == null) {
            return Optional.empty();
        }
        final Optional<Constructor<?>> constructor = findConstructor(clazz, paramsTypes);
        if (!constructor.isPresent()) {
            return Optional.empty();
        }
        try {
            return Optional.of(constructor.get().newInstance(params));
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            LOGGER.warning(String.format(Locale.ENGLISH,
                    "Can not create constructor for class [%s] with params [%s]", clazz.getName(),
                    Arrays.toString(params)));
        }
        return Optional.empty();
    }

    /**
     * Search constructor
     *
     * @param clazz class
     * @param paramsTypes Constructor parameter
     * @return Constructor
     */
    public static Optional<Constructor<?>> findConstructor(Class<?> clazz, Class<?>[] paramsTypes) {
        if (clazz == null) {
            return Optional.empty();
        }

        // Add to constructor cache
        return CONSTRUCTOR_CACHE.computeIfAbsent(buildMethodKey(clazz, "<init>", paramsTypes), key -> {
            try {
                return Optional.of(setObjectAccessible(clazz.getDeclaredConstructor(paramsTypes)));
            } catch (NoSuchMethodException e) {
                LOGGER.warning(String.format(Locale.ENGLISH, "Can not find constructor for class [%s] with params [%s]",
                        clazz.getName(), Arrays.toString(paramsTypes)));
                return Optional.empty();
            }
        });
    }

    private static String buildMethodKey(Class<?> clazz, String methodName, Class<?>[] paramsType) {
        final String name = clazz.getName();
        final StringBuilder sb = new StringBuilder(name.length() + methodName.length() + EXTRA_LENGTH_FOR_METHOD_KEY);
        sb.append(name).append("#").append(methodName).append("(");
        if (paramsType != null) {
            for (Class<?> paramType : paramsType) {
                sb.append(paramType.getName()).append(",");
            }
        }
        return sb.append(")").toString();
    }

    /**
     * Set static field value
     *
     * @param clazz target class
     * @param fieldName field name
     * @param value value
     * @return set result
     */
    public static boolean setStaticFieldValue(Class<?> clazz, String fieldName, Object value) {
        if (clazz == null || StringUtils.isBlank(fieldName)) {
            return false;
        }

        Field field = getField(clazz, fieldName);
        if (field == null) {
            return false;
        }
        if (isFinalField(field)) {
            updateFinalModifierField(field);
        }
        try {
            field.set(null, value);
            return true;
        } catch (IllegalAccessException ex) {
            LOGGER.warning(String.format(Locale.ENGLISH, "Set value for static field [%s] failed! %s", fieldName,
                    ex.getMessage()));
            return false;
        }
    }

    /**
     * Set field value
     *
     * @param target target object
     * @param fieldName field name
     * @param value value
     * @return set result
     */
    public static boolean setFieldValue(Object target, String fieldName, Object value) {
        final Optional<Field> fieldOption = getField(target, fieldName);
        if (!fieldOption.isPresent()) {
            return false;
        }
        final Field field = fieldOption.get();
        if (isFinalField(field)) {
            updateFinalModifierField(field);
        }
        try {
            field.set(target, value);
            return true;
        } catch (IllegalAccessException ex) {
            LOGGER.warning(String.format(Locale.ENGLISH, "Set value for field [%s] failed! %s", fieldName,
                    ex.getMessage()));
            return false;
        }
    }

    /**
     * Update final field
     *
     * @param field Target field
     */
    public static void updateFinalModifierField(Field field) {
        final Field modifiersField = getField(Field.class, "modifiers");
        if (modifiersField != null) {
            try {
                modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
            } catch (IllegalAccessException ex) {
                LOGGER.log(Level.WARNING, String.format(Locale.ENGLISH,
                        "Could not update final field named %s", field.getName()));
            }
        }
    }

    /**
     * Gets the field value by reflection
     *
     * @param target Target object
     * @param fieldName Field name
     * @return value
     */
    public static Optional<Object> getFieldValue(Object target, String fieldName) {
        if (target == null) {
            return Optional.empty();
        }
        return getFieldValueByClazz(target.getClass(), target, fieldName);
    }

    /**
     * Gets the field value by reflection
     *
     * @param className The fully qualified name of the target class
     * @param target Target object
     * @param fieldName Field name
     * @return value
     */
    public static Optional<Object> getFieldValue(String className, Object target, String fieldName) {
        final Optional<Class<?>> clazz = loadClass(className);
        if (clazz.isPresent()) {
            return getFieldValueByClazz(clazz.get(), target, fieldName);
        }
        return Optional.empty();
    }

    /**
     * Gets the field value by reflection
     *
     * @param clazz Target class
     * @param target Target object
     * @param fieldName Field name
     * @return value
     */
    public static Optional<Object> getFieldValueByClazz(Class<?> clazz, Object target, String fieldName) {
        if (fieldName == null) {
            return Optional.empty();
        }
        if (clazz == null && target == null) {
            return Optional.empty();
        }
        Class<?> curClazz = clazz;
        if (curClazz == null) {
            curClazz = target.getClass();
        }
        final Field field = getField(curClazz, fieldName);
        if (field == null) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(field.get(target));
        } catch (IllegalAccessException e) {
            LOGGER.log(Level.WARNING, String.format(Locale.ENGLISH,
                    "Could not acquire the value of field %s", fieldName));
        }
        return Optional.empty();
    }

    /**
     * Check whether the current field is final
     *
     * @param field filed
     * @return result
     */
    private static boolean isFinalField(Field field) {
        return Modifier.isFinal(field.getModifiers());
    }

    private static Optional<Field> getField(Object target, String fieldName) {
        return Optional.ofNullable(getField(target.getClass(), fieldName));
    }

    private static Field getField(Class<?> clazz, String fieldName) {
        final Optional<Field> fieldFromCache = getFieldFromCache(clazz, fieldName);
        if (fieldFromCache.isPresent()) {
            return fieldFromCache.get();
        }
        final Map<String, Field> cache = FIELD_CACHE.getOrDefault(clazz,
                new ConcurrentHashMap<>(INIT_CLASS_FILED_CACHE_SIZE));
        Field field = cache.get(fieldName);
        try {
            if (field == null) {
                field = setObjectAccessible(clazz.getDeclaredField(fieldName));
                cache.putIfAbsent(fieldName, field);
            }
        } catch (IllegalArgumentException | NoSuchFieldException ex) {
            if (clazz.getSuperclass() != null) {
                return getField(clazz.getSuperclass(), fieldName);
            } else {
                LOGGER.log(Level.WARNING, String.format(Locale.ENGLISH,
                        "Could not find field named %s", fieldName));
            }
        } finally {
            FIELD_CACHE.put(clazz, cache);
        }
        return field;
    }

    /**
     * To get the field from the cache, this method will iterate through all the parent classes from the existing cache
     * to get the cache.If it does not exist, obtain it by reflection
     *
     * @param clazz class object
     * @param fieldName field name
     * @return Field
     */
    private static Optional<Field> getFieldFromCache(Class<?> clazz, String fieldName) {
        Class<?> curClazz = clazz;
        while (curClazz != Object.class) {
            final Map<String, Field> cache = FIELD_CACHE.get(curClazz);
            if (cache != null && !cache.isEmpty()) {
                Field field = cache.get(fieldName);
                if (field != null) {
                    return Optional.of(field);
                }
            }
            curClazz = curClazz.getSuperclass();
        }
        return Optional.empty();
    }

    private static <T extends AccessibleObject> T setObjectAccessible(T object) {
        AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
            object.setAccessible(true);
            return object;
        });
        return object;
    }

    /**
     * Gets the field value by reflection
     *
     * @param clazz Target class
     * @param fieldName Field name
     * @return value Field value
     */
    public static Optional<Object> getStaticFieldValue(Class<?> clazz, String fieldName) {
        if (clazz == null || StringUtils.isBlank(fieldName)) {
            return Optional.empty();
        }
        try {
            final Field field = getField(clazz, fieldName);
            if (field != null && Modifier.isStatic(field.getModifiers())) {
                return Optional.ofNullable(field.get(null));
            } else {
                return Optional.empty();
            }
        } catch (IllegalAccessException ex) {
            LOGGER.log(Level.WARNING, String.format(Locale.ENGLISH,
                    "Could not acquire the value of field %s", fieldName));
        }
        return Optional.empty();
    }
}
