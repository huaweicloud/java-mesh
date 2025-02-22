/*
 * Copyright (C) 2024-2024 Sermant Authors. All rights reserved.
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

package io.sermant.implement.config;

import io.sermant.core.classloader.ClassLoaderManager;
import io.sermant.core.exception.SermantRuntimeException;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.Constructor;

/**
 * Yaml constructor for sermant config load
 *
 * @author lilai
 * @since 2024-05-22
 */
public class SermantYamlConstructor extends Constructor {
    private ClassLoader loader;

    /**
     * constructor
     */
    public SermantYamlConstructor() {
        super(Object.class, new LoaderOptions());
        loader = ClassLoaderManager.getContextClassLoaderOrUserClassLoader();
    }

    /**
     * Load class with specific classLoader
     *
     * @param name class name
     * @return created class
     * @throws ClassNotFoundException class not found
     */
    @Override
    protected Class<?> getClassForName(String name) throws ClassNotFoundException {
        return Class.forName(name, true, loader);
    }

    /**
     * set classLoader
     *
     * @param classLoader the classLoader for config object
     * @throws SermantRuntimeException no classloader is provided
     */
    public void setLoader(ClassLoader classLoader) {
        if (classLoader == null) {
            throw new SermantRuntimeException("classLoader must be provided.");
        }
        this.loader = classLoader;
    }

    /**
     * clear snakeyaml class cache
     */
    public void clearCache() {
        typeTags.clear();
    }
}
