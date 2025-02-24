/*
 * Copyright (C) 2024-2024 Sermant Authors. All rights reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package io.sermant.tag.transmission.rpc.interceptors;

import io.sermant.core.plugin.config.PluginConfigManager;
import io.sermant.core.utils.tag.TrafficUtils;
import io.sermant.tag.transmission.config.TagTransmissionConfig;

import org.junit.After;
import org.junit.Before;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * traffic label transparent ut basic test class
 *
 * @author chengyouling
 * @since 2024-12-30
 */
public class BaseInterceptorTest {
    public final TagTransmissionConfig tagTransmissionConfig = new TagTransmissionConfig();

    public MockedStatic<PluginConfigManager> pluginConfigManagerMockedStatic;

    public BaseInterceptorTest() {
        pluginConfigManagerMockedStatic = Mockito.mockStatic(PluginConfigManager.class);
        pluginConfigManagerMockedStatic.when(() -> PluginConfigManager.getPluginConfig(TagTransmissionConfig.class))
                .thenReturn(tagTransmissionConfig);
    }

    @Before
    public void before() {
        tagTransmissionConfig.setEnabled(true);
        List<String> tagKeys = new ArrayList<>();
        tagKeys.add("x_lane_canary");
        Map<String, List<String>> matchRule = new HashMap<>();
        matchRule.put("exact", tagKeys);
        tagTransmissionConfig.setMatchRule(matchRule);
        TrafficUtils.removeTrafficTag();
    }

    @After
    public void after() {
        TrafficUtils.removeTrafficTag();
        pluginConfigManagerMockedStatic.close();
    }
}
