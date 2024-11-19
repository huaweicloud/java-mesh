/*
 * Copyright (C) 2023-2023 Huawei Technologies Co., Ltd. All rights reserved.
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

package io.sermant.core.plugin.classloader;

import io.sermant.core.classloader.ClassLoaderManager;
import io.sermant.core.common.LoggerFactory;
import io.sermant.core.config.ConfigManager;
import io.sermant.core.plugin.agent.config.AgentConfig;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The classloader that loads the main plugin-module of the plugin
 *
 * @author luanwenfei
 * @since 2023-04-27
 */
public class PluginClassLoader extends URLClassLoader {
    /**
     * logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger();

    private final HashMap<Long, ClassLoader> localLoader = new HashMap<>();

    /**
     * Whether to use context classLoader
     */
    private final boolean useContextLoader;

    /**
     * Manages the loaded classes in the classLoader
     */
    private final Map<String, Class<?>> pluginClassMap = new HashMap<>();

    /**
     * constructor
     *
     * @param urls The URL of the lib where the class is to be loaded by the classloader
     * @param parent parent classLoader
     */
    public PluginClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
        useContextLoader = ConfigManager.getConfig(AgentConfig.class).isUseContextLoader();
    }

    /**
     * Load the plugin class and cache it
     *
     * @param name fully qualified name
     * @return class object
     */
    private Class<?> loadPluginClass(String name) {
        if (!pluginClassMap.containsKey(name)) {
            try {
                pluginClassMap.put(name, findClass(name));
            } catch (ClassNotFoundException ignored) {
                pluginClassMap.put(name, null);
            }
        }
        return pluginClassMap.get(name);
    }

    /**
     * Adds the search path for the class to the classloader
     *
     * @param url search path
     */
    public void appendUrl(URL url) {
        this.addURL(url);
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        return this.loadClass(name, false);
    }

    @Override
    public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            Class<?> clazz = loadPluginClass(name);

            // If the class cannot be loaded on its own, it is loaded in the Sermant search path
            if (clazz == null) {
                try {
                    clazz = super.loadClass(name, resolve);
                } catch (ClassNotFoundException e) {
                    // Catch the exception that the class cannot be found. The next step is to load the class by
                    // the localLoader
                    // ignored
                    LOGGER.log(Level.FINE, "Load class failed, msg is {0}", e.getMessage());
                }
            }

            // If the class cannot be found from the Sermant search path, it is attempted to be loaded via the
            // thread-bound localClassLoader
            if (clazz == null) {
                clazz = getClassFromLocalClassLoader(name);
            }

            // If the class cannot be found, an exception is thrown
            if (clazz == null) {
                throw new ClassNotFoundException("Sermant pluginClassLoader can not load class: " + name);
            }

            // Parse the class if necessary
            if (resolve) {
                resolveClass(clazz);
            }
            return clazz;
        }
    }

    private Class<?> getClassFromLocalClassLoader(String name) {
        ClassLoader loader = localLoader.get(Thread.currentThread().getId());
        if (loader == null) {
            LOGGER.log(Level.FINE, "localLoader is null, thread name is {0}, classs name is {1}.",
                    new Object[]{Thread.currentThread().getName(), name});
        }
        if (loader == null && useContextLoader) {
            loader = ClassLoaderManager.getContextClassLoaderOrUserClassLoader();
            if (loader == null) {
                LOGGER.log(Level.WARNING, "contextClassLoader is null, thread name is {0}, classs name is {1}.",
                        new Object[]{Thread.currentThread().getName(), name});
            }
        }
        Class<?> clazz = null;

        // Make sure the localClassLoader is not the current classLoader or ServiceClassLoader, otherwise it
        // will cause stackoverflow
        if (loader != null && !this.equals(loader) && !(loader instanceof ServiceClassLoader)) {
            try {
                clazz = loader.loadClass(name);
            } catch (ClassNotFoundException e) {
                // Class not found, ignored, exception thrown later
                LOGGER.log(Level.FINE, "Load class failed, msg is {0}", e.getMessage());
            }
        }
        return clazz;
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        // Due to class isolation, the service loader does not obtain the service provider from the parent
        // classloader, but returns only the resources in current classloader
        if (name.startsWith("META-INF/services/")) {
            return findResources(name);
        }

        return super.getResources(name);
    }

    /**
     * Load classes only through Sermant's own search path, not using localClassLoader, which would otherwise cause
     * stackoverflow
     *
     * @param name class name
     * @return Class<?>
     * @throws ClassNotFoundException class not found
     */
    public Class<?> loadSermantClass(String name) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            Class<?> clazz = loadPluginClass(name);

            if (clazz == null) {
                try {
                    clazz = super.loadClass(name, false);
                } catch (ClassNotFoundException e) {
                    // Class not found, ignored, exception thrown later
                }
            }

            // If the class cannot be found, an exception is thrown
            if (clazz == null) {
                throw new ClassNotFoundException("Sermant pluginClassLoader can not load class: " + name);
            }
            return clazz;
        }
    }

    /**
     * Set up the localClassLoader
     *
     * @param loader classLoader
     */
    public void setLocalLoader(ClassLoader loader) {
        localLoader.put(Thread.currentThread().getId(), loader);
    }

    /**
     * Clear the localClassLoader
     */
    public void removeLocalLoader() {
        localLoader.remove(Thread.currentThread().getId());
    }
}
