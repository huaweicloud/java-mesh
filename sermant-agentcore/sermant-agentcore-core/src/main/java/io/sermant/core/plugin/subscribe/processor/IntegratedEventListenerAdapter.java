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

package io.sermant.core.plugin.subscribe.processor;

import io.sermant.core.classloader.ClassLoaderManager;
import io.sermant.core.service.dynamicconfig.common.DynamicConfigEvent;
import io.sermant.core.service.dynamicconfig.common.DynamicConfigListener;

/**
 * Listener adapter, multiple listeners are integrated into one processor
 *
 * @author zhouss
 * @since 2022-04-21
 */
public class IntegratedEventListenerAdapter implements DynamicConfigListener {
    private final ConfigProcessor processor;

    private final String rawGroup;

    // Classloader at subscription time
    private final ClassLoader classLoader;

    /**
     * constructor
     *
     * @param processor Configuration processor
     * @param rawGroup Group label
     */
    public IntegratedEventListenerAdapter(ConfigProcessor processor, String rawGroup) {
        this.processor = processor;
        this.rawGroup = rawGroup;
        this.classLoader = ClassLoaderManager.getContextClassLoaderOrUserClassLoader();
    }

    @Override
    public void process(DynamicConfigEvent event) {
        if (processor == null) {
            return;
        }

        // The classloader at subscription time may not be the same as the classloader at listener configuration
        // time, so need to restore it
        ClassLoader currentClassLoader = ClassLoaderManager.getContextClassLoaderOrUserClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(classLoader);
            processor.process(rawGroup, event);
        } finally {
            Thread.currentThread().setContextClassLoader(currentClassLoader);
        }
    }
}
