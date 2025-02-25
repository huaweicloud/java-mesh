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

package io.sermant.router.dubbo.declarer;

import io.sermant.core.plugin.agent.declarer.AbstractPluginDeclarer;
import io.sermant.core.plugin.agent.declarer.InterceptDeclarer;
import io.sermant.core.plugin.agent.matcher.ClassMatcher;
import io.sermant.core.plugin.agent.matcher.MethodMatcher;
import io.sermant.core.plugin.config.PluginConfigManager;
import io.sermant.router.common.config.RouterConfig;
import io.sermant.router.dubbo.interceptor.AlibabaDubboMonitorFilterInterceptor;

/**
 * Enhanced Declarer of the MonitorFilter
 *
 * @author zhp
 * @since 2024-10-23
 */
public class AlibabaDubboMonitorFilterDeclarer extends AbstractPluginDeclarer {
    private static final String ENHANCE_CLASS = "com.alibaba.dubbo.monitor.support.MonitorFilter";

    private static final String METHOD_NAME = "invoke";

    private static final RouterConfig ROUTER_CONFIG = PluginConfigManager.getPluginConfig(RouterConfig.class);

    @Override
    public ClassMatcher getClassMatcher() {
        return ClassMatcher.nameEquals(ENHANCE_CLASS);
    }

    @Override
    public InterceptDeclarer[] getInterceptDeclarers(ClassLoader classLoader) {
        return new InterceptDeclarer[]{
                InterceptDeclarer.build(MethodMatcher.nameEquals(METHOD_NAME),
                        new AlibabaDubboMonitorFilterInterceptor())
        };
    }

    @Override
    public boolean isEnabled() {
        return ROUTER_CONFIG.isEnableMetric();
    }
}
