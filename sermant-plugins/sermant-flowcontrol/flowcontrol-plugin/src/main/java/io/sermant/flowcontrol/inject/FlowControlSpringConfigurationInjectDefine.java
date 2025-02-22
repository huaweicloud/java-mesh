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

package io.sermant.flowcontrol.inject;

import io.sermant.core.classloader.ClassLoaderManager;
import io.sermant.core.service.inject.ClassInjectDefine;
import io.sermant.core.utils.ClassUtils;

/**
 * spring related configuration injection
 *
 * @author zhouss
 * @since 2022-06-28
 */
public class FlowControlSpringConfigurationInjectDefine implements ClassInjectDefine {
    @Override
    public String injectClassName() {
        return "io.sermant.flowcontrol.inject.FlowControlSpringConfiguration";
    }

    @Override
    public String factoryName() {
        return ENABLE_AUTO_CONFIGURATION_FACTORY_NAME;
    }

    @Override
    public ClassInjectDefine[] requiredDefines() {
        return new ClassInjectDefine[]{
                this.build("io.sermant.flowcontrol.inject.DefaultClientHttpResponse", null,
                    () -> isLoadedClass("org.springframework.http.client.ClientHttpResponse")),
                this.build("io.sermant.flowcontrol.inject.RetryClientHttpResponse", null,
                    () -> isLoadedClass("org.springframework.http.client.AbstractClientHttpResponse"))
        };
    }

    @Override
    public Plugin plugin() {
        return Plugin.FLOW_CONTROL_PLUGIN;
    }

    private boolean isLoadedClass(String className) {
        return ClassUtils
                .loadClass(className, ClassLoaderManager.getContextClassLoaderOrUserClassLoader(), true)
                .isPresent();
    }
}
