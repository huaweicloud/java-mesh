/*
 * Copyright (C) 2022-2022 Huawei Technologies Co., Ltd. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Locale;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ClassUtils
 *
 * @author zhouss
 * @since 2022-04-08
 */
public class ClassUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger();

    private ClassUtils() {
    }

    /**
     * Load the class by specifying the classloader
     *
     * @param className Class fully qualified name
     * @param classLoader The classloader of the enhanced class
     * @return defined class
     */
    public static Optional<Class<?>> defineClass(String className, ClassLoader classLoader) {
        if (classLoader == null || className == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(ClassLoaderUtils.defineClass(className, classLoader, ClassLoaderUtils
                    .getClassResource(ClassLoader.getSystemClassLoader(), className)));
        } catch (InvocationTargetException ex) {
            LOGGER.fine(String.format(Locale.ENGLISH, "Can not define class [%s], reason: [%s], may be it has been "
                    + "defined, so try to load it!", className, ex.getMessage()));
        } catch (IllegalAccessException | NoSuchMethodException | IOException ex) {
            LOGGER.warning(String.format(Locale.ENGLISH, "Can not define class [%s], reason: [%s]", className,
                    ex.getMessage()));
        }

        // It may have already been loaded. Use contextClassLoader.loadClass to load it
        return loadClass(className, classLoader);
    }

    /**
     * load class
     *
     * @param className Class fully qualified name
     * @param classLoader classLoader
     * @return loaded class
     */
    public static Optional<Class<?>> loadClass(String className, ClassLoader classLoader) {
        return loadClass(className, classLoader, true);
    }

    /**
     * load class
     *
     * @param className Class fully qualified name
     * @param classLoader classLoader
     * @param isNeedWarn is need warn
     * @return loaded class
     */
    public static Optional<Class<?>> loadClass(String className, ClassLoader classLoader, boolean isNeedWarn) {
        if (classLoader == null || className == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(classLoader.loadClass(className));
        } catch (ClassNotFoundException | NoClassDefFoundError ignored) {
            final String message = String.format(Locale.ENGLISH, "Can not load class [%s]!", className);
            if (isNeedWarn) {
                LOGGER.log(Level.WARNING, message, ignored);
            } else {
                LOGGER.fine(message);
            }
        }
        return Optional.empty();
    }

    /**
     * Reflection creation instance
     *
     * @param className Class fully qualified name
     * @param classLoader classLoader
     * @param paramTypes param types
     * @return Created object
     */
    public static Optional<Object> createInstance(String className, ClassLoader classLoader, Class<?>[] paramTypes) {
        ClassLoader curClassLoader = classLoader;
        if (curClassLoader == null) {
            curClassLoader = ClassLoaderManager.getContextClassLoaderOrUserClassLoader();
        }
        try {
            final Class<?> clazz = curClassLoader.loadClass(className);
            return Optional.of(clazz.getDeclaredConstructor(paramTypes));
        } catch (ClassNotFoundException | NoSuchMethodException exception) {
            LOGGER.warning(String.format(Locale.ENGLISH, "Can not find class named [%s] for classloader [%s]",
                    className, classLoader));
        }
        return Optional.empty();
    }
}
