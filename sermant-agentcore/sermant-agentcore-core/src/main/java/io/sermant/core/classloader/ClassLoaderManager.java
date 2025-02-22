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

package io.sermant.core.classloader;

import io.sermant.core.common.CommonConstant;
import io.sermant.core.exception.FileCheckException;
import io.sermant.core.plugin.classloader.PluginClassFinder;
import io.sermant.core.plugin.classloader.PluginClassLoader;
import io.sermant.core.utils.FileUtils;
import io.sermant.god.common.SermantClassLoader;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Manage custom classloaders.
 *
 * @author luanwenfei
 * @since 2022-06-20
 */
public class ClassLoaderManager {
    private static SermantClassLoader sermantClassLoader;

    private static PluginClassFinder pluginClassFinder;

    private static FrameworkClassLoader frameworkClassLoader;

    private static ClassLoader userClassLoader;

    private ClassLoaderManager() {
    }

    /**
     * Init custom classloaders.
     *
     * @param argsMap arguments map
     * @throws MalformedURLException MalformedURLException
     */
    public static void init(Map<String, Object> argsMap) throws MalformedURLException {
        sermantClassLoader = (SermantClassLoader) ClassLoaderManager.class.getClassLoader();

        // Load the contents of the Common package into the SermantClassLoader
        // The third-party dependencies introduced here need to control that the current dependency is not a host
        // instance dependency that needs to be used in the enhancement, otherwise, a type conversion error will occur
        sermantClassLoader
                .appendUrls(listCommonLibUrls(argsMap.get(CommonConstant.COMMON_DEPENDENCY_DIR_KEY).toString()));
        frameworkClassLoader = initFrameworkClassLoader(argsMap.get(CommonConstant.CORE_IMPLEMENT_DIR_KEY).toString());
        pluginClassFinder = new PluginClassFinder();
    }

    private static FrameworkClassLoader initFrameworkClassLoader(String path) throws MalformedURLException {
        if (frameworkClassLoader != null) {
            return frameworkClassLoader;
        }
        URL[] coreImplementUrls = listCoreImplementUrls(path);
        return new FrameworkClassLoader(coreImplementUrls, sermantClassLoader);
    }

    /**
     * Create a PluginClassLoader
     *
     * @return PluginClassLoader
     */
    public static PluginClassLoader createPluginClassLoader() {
        return AccessController.doPrivileged(new PrivilegedAction<PluginClassLoader>() {
            @Override
            public PluginClassLoader run() {
                return new PluginClassLoader(new URL[0], sermantClassLoader);
            }
        });
    }

    public static void setUserClassLoader(ClassLoader userClassLoader) {
        ClassLoaderManager.userClassLoader = userClassLoader;
    }

    /**
     * get ContextClassLoader or UserClassLoader
     *
     * @return ClassLoader
     */
    public static ClassLoader getContextClassLoaderOrUserClassLoader() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader != null) {
            return classLoader;
        }
        return userClassLoader;
    }

    public static SermantClassLoader getSermantClassLoader() {
        return sermantClassLoader;
    }

    /**
     * For getting FrameworkClassLoader
     *
     * @return A frameworkClassLoader that has been initialized.
     */
    public static FrameworkClassLoader getFrameworkClassLoader() {
        return frameworkClassLoader;
    }

    public static PluginClassFinder getPluginClassFinder() {
        return pluginClassFinder;
    }

    private static URL[] listCoreImplementUrls(String coreImplementPath) throws MalformedURLException {
        File coreImplementDir = new File(FileUtils.validatePath(coreImplementPath));
        if (!coreImplementDir.exists() || !coreImplementDir.isDirectory()) {
            throw new FileCheckException("core implement directory is not exist or is not directory.");
        }
        File[] jars = coreImplementDir.listFiles((file, name) -> name.endsWith(".jar"));
        if (jars == null || jars.length == 0) {
            throw new FileCheckException("core implement directory is empty");
        }
        List<URL> urlList = new ArrayList<>();
        for (File jar : jars) {
            urlList.add(jar.toURI().toURL());
        }
        return urlList.toArray(new URL[0]);
    }

    private static List<URL> listCommonLibUrls(String commonLibPath) throws MalformedURLException {
        File commonLibDir = new File(FileUtils.validatePath(commonLibPath));
        if (!commonLibDir.exists() || !commonLibDir.isDirectory()) {
            throw new FileCheckException("common lib is not exist or is not directory.");
        }
        File[] jars = commonLibDir.listFiles((file, name) -> name.endsWith(".jar"));
        if (jars == null || jars.length == 0) {
            throw new FileCheckException("common lib directory is empty");
        }
        List<URL> urlList = new ArrayList<>();
        for (File jar : jars) {
            urlList.add(jar.toURI().toURL());
        }
        return urlList;
    }
}
