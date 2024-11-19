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

package io.sermant.registry.inject;

import io.sermant.core.classloader.ClassLoaderManager;
import io.sermant.core.plugin.config.PluginConfigManager;
import io.sermant.core.service.inject.ClassInjectDefine;
import io.sermant.core.utils.ClassUtils;
import io.sermant.registry.config.RegisterConfig;

/**
 * configuration
 *
 * @author zhouss
 * @since 2022-05-19
 */
public abstract class BaseAutoConfigurationDefine implements ClassInjectDefine {
    @Override
    public boolean canInject() {
        final RegisterConfig pluginConfig = PluginConfigManager.getPluginConfig(RegisterConfig.class);
        return pluginConfig.isEnableSpringRegister() && !pluginConfig.isOpenMigration();
    }

    @Override
    public Plugin plugin() {
        return Plugin.SPRING_REGISTRY_PLUGIN;
    }

    /**
     * Specifies whether the class is loaded by the current classloader
     *
     * @param className Class full limited name
     * @return Whether it is loaded or not
     */
    protected boolean isClassExistedOnCurrentClassLoader(String className) {
        return ClassUtils.loadClass(className, ClassLoaderManager.getContextClassLoaderOrUserClassLoader(),
                false).isPresent();
    }
}
