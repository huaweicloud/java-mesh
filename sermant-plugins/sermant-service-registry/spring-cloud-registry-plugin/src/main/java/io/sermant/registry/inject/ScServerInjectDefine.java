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
 * Ribbon Server Injection
 *
 * @author zhouss
 * @since 2022-05-31
 */
public class ScServerInjectDefine implements ClassInjectDefine {
    @Override
    public String injectClassName() {
        return "io.sermant.registry.entity.ScServer";
    }

    @Override
    public String factoryName() {
        return "";
    }

    @Override
    public boolean canInject() {
        final ClassLoader contextClassLoader = ClassLoaderManager.getContextClassLoaderOrUserClassLoader();
        final RegisterConfig pluginConfig = PluginConfigManager.getPluginConfig(RegisterConfig.class);
        return pluginConfig.isOpenMigration() && pluginConfig.isEnableSpringRegister()
                && ClassUtils.loadClass("com.netflix.loadbalancer.Server", contextClassLoader).isPresent();
    }

    @Override
    public ClassInjectDefine[] requiredDefines() {
        return new ClassInjectDefine[] {
                build("io.sermant.registry.auto.sc.ServiceCombServerMetaInfo", null)
        };
    }

    @Override
    public Plugin plugin() {
        return Plugin.SPRING_REGISTRY_PLUGIN;
    }
}
