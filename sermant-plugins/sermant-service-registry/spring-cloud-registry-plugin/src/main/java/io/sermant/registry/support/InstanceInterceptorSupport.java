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

package io.sermant.registry.support;

import io.sermant.core.classloader.ClassLoaderManager;
import io.sermant.core.common.LoggerFactory;
import io.sermant.core.plugin.config.PluginConfigManager;
import io.sermant.core.utils.ClassLoaderUtils;
import io.sermant.core.utils.StringUtils;
import io.sermant.registry.config.RegisterConfig;
import io.sermant.registry.entity.MicroServiceInstance;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The instance is supported by interceptors
 *
 * @author zhouss
 * @since 2022-02-22
 */
public abstract class InstanceInterceptorSupport extends RegisterSwitchSupport {
    private static final Logger LOGGER = LoggerFactory.getLogger();

    /**
     * Class caching to avoid multiple calls to loadClass
     */
    private final Map<String, Class<?>> cacheClasses = new ConcurrentHashMap<>();

    private RegisterConfig config;

    /**
     * Whether to enable registry migration and dual registration
     *
     * @return Whether it is turned on
     */
    protected final boolean isOpenMigration() {
        return getRegisterConfig().isOpenMigration();
    }

    private RegisterConfig getRegisterConfig() {
        if (config == null) {
            config = PluginConfigManager.getPluginConfig(RegisterConfig.class);
        }
        return config;
    }

    /**
     * Get an instance class object When it doesn't exist, it's loaded with a host class loader so that it can be
     * associated with the host
     *
     * @param className The host is a fully qualified class name
     * @return Host class
     */
    protected final Class<?> getInstanceClass(String className) {
        return cacheClasses.computeIfAbsent(className, fn -> {
            ClassLoader contextClassLoader = ClassLoaderManager.getContextClassLoaderOrUserClassLoader();
            Class<?> result = null;
            try {
                result = ClassLoaderUtils.defineClass(className, contextClassLoader,
                        ClassLoaderUtils.getClassResource(this.getClass().getClassLoader(), className));
            } catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException | IOException e) {
                // It may have already been loaded, and it can be loaded directly with contextClassLoader.loadClass
                try {
                    result = contextClassLoader.loadClass(className);
                } catch (ClassNotFoundException ignored) {
                    LOGGER.log(Level.WARNING, "{0} class not found.", className);
                }
            }
            return result;
        });
    }

    /**
     * Build instances are transformed by the subclasses themselves
     *
     * @param microServiceInstance Instance information
     * @param serviceName Service name
     * @return Object
     */
    protected final Optional<Object> buildInstance(MicroServiceInstance microServiceInstance, String serviceName) {
        final Class<?> serverClass = getInstanceClass(getInstanceClassName());
        try {
            Constructor<?> declaredConstructor = serverClass
                    .getDeclaredConstructor(MicroServiceInstance.class, String.class);
            return Optional.of(declaredConstructor.newInstance(microServiceInstance, serviceName));
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException
            | InvocationTargetException exception) {
            return Optional.empty();
        }
    }

    /**
     * Whether the host is a WebFlux application
     *
     * @param target Enhance the target
     * @return Yes returns true
     */
    protected boolean isWebfLux(Object target) {
        return StringUtils.equals("org.springframework.cloud.client.discovery.composite.reactive"
                + ".ReactiveCompositeDiscoveryClient", target.getClass().getName());
    }

    /**
     * Obtain the name of the instance class
     *
     * @return The name of the instance class is specified
     */
    protected abstract String getInstanceClassName();
}
