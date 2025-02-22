/*
 * Copyright (C) 2022-2022 Huawei Technologies Co., Ltd. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.sermant.dubbo.registry.utils;

import io.sermant.core.classloader.ClassLoaderManager;
import io.sermant.core.common.LoggerFactory;
import io.sermant.core.utils.ClassLoaderUtils;
import io.sermant.dubbo.registry.cache.DubboCache;
import io.sermant.dubbo.registry.constants.Constant;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * In order to be compatible with both Alibaba and Apache Dubbo, you need to use the reflection method to perform the
 * class operation
 *
 * @author provenceee
 * @since 2022-02-07
 */
public class ReflectUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger();

    private static final String GET_PROTOCOL_METHOD_NAME = "getProtocol";

    private static final String GET_ADDRESS_METHOD_NAME = "getAddress";

    private static final String GET_PATH_METHOD_NAME = "getPath";

    private static final String GET_ID_METHOD_NAME = "getId";

    private static final String GET_NAME_METHOD_NAME = "getName";

    private static final String GET_PARAMETERS_METHOD_NAME = "getParameters";

    private static final String GET_REGISTRIES_METHOD_NAME = "getRegistries";

    private static final String GET_EXTENSION_CLASSES_METHOD_NAME = "getExtensionClasses";

    private static final String IS_VALID_METHOD_NAME = "isValid";

    private static final String SET_HOST_METHOD_NAME = "setHost";

    private static final String SET_ADDRESS_METHOD_NAME = "setAddress";

    private static final String SET_PATH_METHOD_NAME = "setPath";

    private static final String SET_ID_METHOD_NAME = "setId";

    private static final String SET_PREFIX_METHOD_NAME = "setPrefix";

    private static final String SET_PROTOCOL_METHOD_NAME = "setProtocol";

    private static final String NOTIFY_METHOD_NAME = "notify";

    private static final String VALUE_OF_METHOD_NAME = "valueOf";

    private static final String REMOVE_PARAMETERS_METHOD_NAME = "removeParameters";

    private static final String ADD_PARAMETERS_METHOD_NAME = "addParameters";

    private static final String GET_PARAMETER_METHOD_NAME = "getParameter";

    private static final String GET_HOST_METHOD_NAME = "getHost";

    private static final String GET_PORT_METHOD_NAME = "getPort";

    private static final String GET_SERVICE_INTERFACE_METHOD_NAME = "getServiceInterface";

    private ReflectUtils() {
    }

    /**
     * Load the host class
     *
     * @param className The host is a fully qualified class name
     * @return Host class
     */
    public static Optional<Class<?>> defineClass(String className) {
        ClassLoader contextClassLoader = ClassLoaderManager.getContextClassLoaderOrUserClassLoader();
        try {
            return Optional.of(ClassLoaderUtils.defineClass(className, contextClassLoader,
                    ClassLoaderUtils.getClassResource(ReflectUtils.class.getClassLoader(), className)));
        } catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException | IOException e) {
            // It may have already been loaded, and it can be loaded directly with context Class Loader.load Class
            try {
                return Optional.of(contextClassLoader.loadClass(className));
            } catch (ClassNotFoundException ex) {
                return Optional.empty();
            }
        }
    }

    /**
     * Create a registration configuration
     *
     * @param clazz RegistryConfig(apache/alibaba)
     * @param <T> RegistryConfig(apache/alibaba)
     * @return registry config
     * @see com.alibaba.dubbo.config.RegistryConfig
     * @see org.apache.dubbo.config.RegistryConfig
     */
    public static <T> Optional<T> newRegistryConfig(Class<T> clazz) {
        try {
            Constructor<T> constructor = clazz.getConstructor(String.class);

            // This URL is not important, what matters is the protocol, so set it to localhost:30100
            return Optional.of(constructor.newInstance(Constant.SC_REGISTRY_ADDRESS));
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException
            | InvocationTargetException e) {
            LOGGER.log(Level.SEVERE, "Cannot new the registryConfig.", e);
            return Optional.empty();
        }
    }

    /**
     * Get the protocol
     *
     * @param obj RegistryConfig | URL
     * @return Protocol
     * @see com.alibaba.dubbo.config.RegistryConfig
     * @see org.apache.dubbo.config.RegistryConfig
     * @see com.alibaba.dubbo.common.URL
     * @see org.apache.dubbo.common.URL
     */
    public static String getProtocol(Object obj) {
        return invokeWithNoneParameterAndReturnString(obj, GET_PROTOCOL_METHOD_NAME);
    }

    /**
     * Obtain the application address
     *
     * @param obj URL
     * @return The address of the application
     * @see com.alibaba.dubbo.common.URL
     * @see org.apache.dubbo.common.URL
     */
    public static String getAddress(Object obj) {
        return invokeWithNoneParameterAndReturnString(obj, GET_ADDRESS_METHOD_NAME);
    }

    /**
     * Obtain the path name, which is usually the same as the interface name. 2.6. x, 2.7.0-2.7.7 In multi
     * implementation scenarios, a sequence number will be spelled after the interface name
     *
     * @param obj URL
     * @return Interface
     * @see com.alibaba.dubbo.common.URL
     * @see org.apache.dubbo.common.URL
     */
    public static String getPath(Object obj) {
        return invokeWithNoneParameterAndReturnString(obj, GET_PATH_METHOD_NAME);
    }

    /**
     * Get the registration ID
     *
     * @param obj RegistryConfig
     * @return Registration ID
     * @see com.alibaba.dubbo.config.RegistryConfig
     * @see org.apache.dubbo.config.RegistryConfig
     */
    public static String getId(Object obj) {
        return invokeWithNoneParameterAndReturnString(obj, GET_ID_METHOD_NAME);
    }

    /**
     * Obtain the name of the dubbo application
     *
     * @param obj ApplicationConfig
     * @return The name of the app
     * @see com.alibaba.dubbo.config.ApplicationConfig
     * @see org.apache.dubbo.config.ApplicationConfig
     */
    public static String getName(Object obj) {
        return invokeWithNoneParameterAndReturnString(obj, GET_NAME_METHOD_NAME);
    }

    /**
     * Obtain the API name
     *
     * @param obj URL
     * @return The name of the API
     * @see com.alibaba.dubbo.common.URL
     * @see org.apache.dubbo.common.URL
     */
    public static String getServiceInterface(Object obj) {
        return invokeWithNoneParameterAndReturnString(obj, GET_SERVICE_INTERFACE_METHOD_NAME);
    }

    /**
     * Get the app host
     *
     * @param obj URL
     * @return Application hosts
     * @see com.alibaba.dubbo.common.URL
     * @see org.apache.dubbo.common.URL
     */
    public static String getHost(Object obj) {
        return invokeWithNoneParameterAndReturnString(obj, GET_HOST_METHOD_NAME);
    }

    /**
     * Obtain the application port
     *
     * @param obj URL
     * @return Application port
     * @see com.alibaba.dubbo.common.URL
     * @see org.apache.dubbo.common.URL
     */
    public static int getPort(Object obj) {
        return invokeWithNoneParameterAndReturnInteger(obj, GET_PORT_METHOD_NAME);
    }

    /**
     * Obtain the url parameter
     *
     * @param obj ApplicationConfig
     * @param key key
     * @return The name of the app
     * @see com.alibaba.dubbo.config.ApplicationConfig
     * @see org.apache.dubbo.config.ApplicationConfig
     */
    public static String getParameter(Object obj, String key) {
        return invokeWithStringParameter(obj, GET_PARAMETER_METHOD_NAME, key, String.class);
    }

    /**
     * Obtain the url parameter
     *
     * @param obj URL
     * @return url parameters
     * @see com.alibaba.dubbo.common.URL
     * @see org.apache.dubbo.common.URL
     */
    public static Map<String, String> getParameters(Object obj) {
        return invokeWithNoneParameter(obj, GET_PARAMETERS_METHOD_NAME, Map.class, true);
    }

    /**
     * Get a list of registration information
     *
     * @param obj AbstractInterfaceConfig
     * @return List of registration information
     * @see com.alibaba.dubbo.config.AbstractInterfaceConfig
     * @see org.apache.dubbo.config.AbstractInterfaceConfig
     */
    public static List<Object> getRegistries(Object obj) {
        return invokeWithNoneParameter(obj, GET_REGISTRIES_METHOD_NAME, List.class, true);
    }

    /**
     * Get Dubbo SPI cache class
     *
     * @param obj ExtensionLoader
     * @return Cached lists
     * @see com.alibaba.dubbo.common.extension.ExtensionLoader
     * @see org.apache.dubbo.common.extension.ExtensionLoader
     */
    public static Map<String, Class<?>> getExtensionClasses(Object obj) {
        return invokeWithNoneParameter(obj, GET_EXTENSION_CLASSES_METHOD_NAME, Map.class, false);
    }

    /**
     * Determine whether the registration information is valid
     *
     * @param obj RegistryConfig
     * @return Whether it works
     * @see com.alibaba.dubbo.config.RegistryConfig
     * @see org.apache.dubbo.config.RegistryConfig
     */
    public static boolean isValid(Object obj) {
        Boolean isValid = invokeWithNoneParameter(obj, IS_VALID_METHOD_NAME, Boolean.class, true);
        if (isValid == null) {
            // null means that there is no such method, and returns true
            return true;
        }
        return isValid;
    }

    /**
     * Set the host
     *
     * @param obj URL
     * @param host host
     * @return URL
     * @see com.alibaba.dubbo.common.URL
     * @see org.apache.dubbo.common.URL
     */
    public static Object setHost(Object obj, String host) {
        return invokeWithStringParameter(obj, SET_HOST_METHOD_NAME, host);
    }

    /**
     * Set the address
     *
     * @param obj URL
     * @param address 地址
     * @return URL
     * @see com.alibaba.dubbo.common.URL
     * @see org.apache.dubbo.common.URL
     */
    public static Object setAddress(Object obj, String address) {
        return invokeWithStringParameter(obj, SET_ADDRESS_METHOD_NAME, address);
    }

    /**
     * Set the path
     *
     * @param obj URL
     * @param path Path
     * @return URL
     * @see com.alibaba.dubbo.common.URL
     * @see org.apache.dubbo.common.URL
     */
    public static Object setPath(Object obj, String path) {
        return invokeWithStringParameter(obj, SET_PATH_METHOD_NAME, path);
    }

    /**
     * Set the ID
     *
     * @param obj RegistryConfig
     * @param id id
     * @see com.alibaba.dubbo.config.RegistryConfig
     * @see org.apache.dubbo.config.RegistryConfig
     */
    public static void setId(Object obj, String id) {
        invokeWithStringParameter(obj, SET_ID_METHOD_NAME, id);
    }

    /**
     * Set the prefix
     *
     * @param obj RegistryConfig
     * @param prefix Prefix
     * @see com.alibaba.dubbo.config.RegistryConfig
     * @see org.apache.dubbo.config.RegistryConfig
     */
    public static void setPrefix(Object obj, String prefix) {
        invokeWithStringParameter(obj, SET_PREFIX_METHOD_NAME, prefix);
    }

    /**
     * Set Protocol
     *
     * @param obj URL
     * @param protocol Protocol
     * @return URL
     * @see com.alibaba.dubbo.common.URL
     * @see org.apache.dubbo.common.URL
     */
    public static Object setProtocol(Object obj, String protocol) {
        return invokeWithStringParameter(obj, SET_PROTOCOL_METHOD_NAME, protocol);
    }

    /**
     * Notify the URL of the downstream interface of Dubbo
     *
     * @param notifyListener Notification Listener
     * @param urls URL of the downstream interface
     * @see com.alibaba.dubbo.registry.NotifyListener
     * @see org.apache.dubbo.registry.NotifyListener
     * @see com.alibaba.dubbo.common.URL
     * @see org.apache.dubbo.common.URL
     */
    public static void notify(Object notifyListener, List<Object> urls) {
        invokeWithParameter(notifyListener, NOTIFY_METHOD_NAME, urls, List.class);
    }

    /**
     * Create a URL based on the address
     *
     * @param address Address
     * @return URL
     * @see com.alibaba.dubbo.common.URL
     * @see org.apache.dubbo.common.URL
     */
    public static Object valueOf(String address) {
        return invoke(new InvokeParameter(DubboCache.INSTANCE.getUrlClass(), null, VALUE_OF_METHOD_NAME, address,
                String.class)).orElse(null);
    }

    /**
     * Delete the parameters in the URL
     *
     * @param url URL
     * @param keys The key of the parameter to be deleted
     * @return url
     * @see com.alibaba.dubbo.common.URL
     * @see org.apache.dubbo.common.URL
     */
    public static Object removeParameters(Object url, Collection<String> keys) {
        return invokeWithParameter(url, REMOVE_PARAMETERS_METHOD_NAME, keys, Collection.class);
    }

    /**
     * Add parameters to the URL
     *
     * @param url URL
     * @param parameters Parameters that need to be added
     * @return url
     * @see com.alibaba.dubbo.common.URL
     * @see org.apache.dubbo.common.URL
     */
    public static Object addParameters(Object url, Map<String, String> parameters) {
        return invokeWithParameter(url, ADD_PARAMETERS_METHOD_NAME, parameters, Map.class);
    }

    private static String invokeWithNoneParameterAndReturnString(Object obj, String name) {
        return invokeWithNoneParameter(obj, name, String.class, true);
    }

    private static int invokeWithNoneParameterAndReturnInteger(Object obj, String name) {
        return invokeWithNoneParameter(obj, name, Integer.class, true);
    }

    private static <T> T invokeWithNoneParameter(Object obj, String name, Class<T> returnClass, boolean isPublic) {
        InvokeParameter invokeParameter = new InvokeParameter(obj.getClass(), obj, name, null, null);
        invokeParameter.isPublic = isPublic;
        return returnClass.cast(invoke(invokeParameter).orElse(null));
    }

    private static <T> T invokeWithStringParameter(Object obj, String name, String parameter, Class<T> returnClass) {
        InvokeParameter invokeParameter = new InvokeParameter(obj.getClass(), obj, name, parameter, String.class);
        invokeParameter.isPublic = true;
        return returnClass.cast(invoke(invokeParameter).orElse(null));
    }

    private static Object invokeWithStringParameter(Object obj, String name, String parameter) {
        return invokeWithParameter(obj, name, parameter, String.class);
    }

    private static Object invokeWithParameter(Object obj, String name, Object parameter, Class<?> parameterClass) {
        return invoke(new InvokeParameter(obj.getClass(), obj, name, parameter, parameterClass)).orElse(null);
    }

    private static Optional<Object> invoke(InvokeParameter parameter) {
        try {
            Method method = getMethod(parameter.invokeClass, parameter.name, parameter.parameterClass,
                    parameter.isPublic);
            if (parameter.parameter == null) {
                return Optional.ofNullable(method.invoke(parameter.obj));
            }
            return Optional.ofNullable(method.invoke(parameter.obj, parameter.parameter));
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            // Due to version limitations, it is possible that methods may not be found, so these errors can be ignored
            return Optional.empty();
        }
    }

    private static Method getMethod(Class<?> invokeClass, String name, Class<?> parameterClass, boolean isPublic)
            throws NoSuchMethodException {
        boolean hasParameter = parameterClass != null;
        if (hasParameter && isPublic) {
            // Public methods with parameters
            return invokeClass.getMethod(name, parameterClass);
        }
        if (hasParameter) {
            // Non public methods with parameters
            return setAccessible(invokeClass.getDeclaredMethod(name, parameterClass));
        }
        if (isPublic) {
            // Public method without parameters
            return invokeClass.getMethod(name);
        }

        // Non public method without parameters
        return setAccessible(invokeClass.getDeclaredMethod(name));
    }

    private static Method setAccessible(Method method) {
        AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
            method.setAccessible(true);
            return method;
        });
        return method;
    }

    /**
     * Reflection parameters
     *
     * @since 2022-02-07
     */
    private static class InvokeParameter {
        Class<?> invokeClass;

        Object obj;

        String name;

        Object parameter;

        Class<?> parameterClass;

        boolean isPublic;

        InvokeParameter(Class<?> invokeClass, Object obj, String name, Object parameter, Class<?> parameterClass) {
            this.invokeClass = invokeClass;
            this.obj = obj;
            this.name = name;
            this.parameter = parameter;
            this.parameterClass = parameterClass;
            this.isPublic = true;
        }
    }
}
