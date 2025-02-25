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

package io.sermant.agentcore.tests.plugin.interceptor.configload;

import io.sermant.agentcore.tests.plugin.config.TestConfig;
import io.sermant.agentcore.tests.plugin.constants.TestConstants;
import io.sermant.core.common.LoggerFactory;
import io.sermant.core.config.ConfigManager;
import io.sermant.core.plugin.agent.entity.ExecuteContext;
import io.sermant.core.plugin.agent.interceptor.AbstractInterceptor;
import io.sermant.core.plugin.config.PluginConfigManager;
import io.sermant.core.plugin.config.ServiceMeta;
import io.sermant.core.utils.StringUtils;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 配置获取拦截器
 *
 * @author tangle
 * @since 2023-10-09
 */
public class ConfigLoadInterceptor extends AbstractInterceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger();

    private final ServiceMeta serviceMeta = ConfigManager.getConfig(ServiceMeta.class);

    @Override
    public ExecuteContext before(ExecuteContext context) {
        String pluginConfigValue = PluginConfigManager.getPluginConfig(TestConfig.class).getTestConfigValue();
        if (!StringUtils.isEmpty(pluginConfigValue) && "test-config-value".equals(pluginConfigValue)) {
            context.getArguments()[TestConstants.PARAM_INDEX_0] = true;
        }
        String coreConfigValue = serviceMeta.getApplication();
        if (!StringUtils.isEmpty(coreConfigValue) && "testApplication".equals(coreConfigValue)) {
            context.getArguments()[TestConstants.PARAM_INDEX_1] = true;
        }
        LOGGER.log(Level.INFO,
                "Test config load, core config 'service.meta.application' is {0}, "
                        + "plugin config 'test-config-value' is {1}",
                new String[]{coreConfigValue, pluginConfigValue});
        return context;
    }

    @Override
    public ExecuteContext after(ExecuteContext context) {
        return context;
    }
}
