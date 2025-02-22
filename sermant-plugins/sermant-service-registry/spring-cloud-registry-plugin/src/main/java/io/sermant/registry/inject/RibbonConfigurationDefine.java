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
import io.sermant.core.service.inject.ClassInjectDefine;
import io.sermant.core.utils.ClassUtils;

/**
 * Ribbon is automatically configured for injection
 *
 * @author zhouss
 * @since 2022-05-19
 */
public class RibbonConfigurationDefine extends BaseAutoConfigurationDefine {
    @Override
    public String injectClassName() {
        return "io.sermant.registry.auto.sc.configuration.ServiceCombRibbonAutoConfiguration";
    }

    @Override
    public String factoryName() {
        return ENABLE_AUTO_CONFIGURATION_FACTORY_NAME;
    }

    @Override
    public ClassInjectDefine[] requiredDefines() {
        return new ClassInjectDefine[]{
                build("io.sermant.registry.auto.sc.ServiceCombServerMetaInfo", null),
                build("io.sermant.registry.auto.sc.ServiceCombServer", null),
                build("io.sermant.registry.auto.sc.ServiceCombServiceList", null),
                build("io.sermant.registry.auto.sc.ServiceCombServerIntrospector", null),
                build("io.sermant.registry.auto.sc.configuration.ServiceCombRibbonConfiguration", null)
        };
    }

    @Override
    public boolean canInject() {
        return super.canInject() && ClassUtils.loadClass(
                "org.springframework.cloud.netflix.ribbon.SpringClientFactory",
                ClassLoaderManager.getContextClassLoaderOrUserClassLoader(), false).isPresent();
    }
}
