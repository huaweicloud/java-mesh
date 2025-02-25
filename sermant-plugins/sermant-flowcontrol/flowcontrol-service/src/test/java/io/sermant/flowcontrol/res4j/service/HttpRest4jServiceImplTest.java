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

package io.sermant.flowcontrol.res4j.service;

import io.sermant.core.operation.OperationManager;
import io.sermant.core.operation.converter.api.YamlConverter;
import io.sermant.core.plugin.config.PluginConfigManager;
import io.sermant.flowcontrol.common.config.FlowControlConfig;
import io.sermant.flowcontrol.common.config.XdsFlowControlConfig;
import io.sermant.flowcontrol.common.entity.FlowControlResult;
import io.sermant.flowcontrol.common.entity.HttpRequestEntity;
import io.sermant.flowcontrol.res4j.chain.context.ChainContext;
import io.sermant.implement.operation.converter.YamlConverterImpl;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * http request test
 *
 * @author zhouss
 * @since 2022-08-30
 */
public class HttpRest4jServiceImplTest {
    private MockedStatic<PluginConfigManager> pluginConfigManagerMockedStatic;

    private MockedStatic<OperationManager> operationManagerMockedStatic;

    @Before
    public void setUp() {
        pluginConfigManagerMockedStatic = Mockito.mockStatic(PluginConfigManager.class);
        pluginConfigManagerMockedStatic.when(() -> PluginConfigManager.getPluginConfig(FlowControlConfig.class))
                .thenReturn(new FlowControlConfig());
        pluginConfigManagerMockedStatic.when(() -> PluginConfigManager.getPluginConfig(XdsFlowControlConfig.class))
                .thenReturn(new XdsFlowControlConfig());
        operationManagerMockedStatic = Mockito.mockStatic(OperationManager.class);
        operationManagerMockedStatic.when(() -> OperationManager.getOperation(YamlConverter.class)).thenReturn(new YamlConverterImpl());
    }

    /**
     * test process
     */
    @Test
    public void test() {
        final HttpRest4jServiceImpl httpRest4jService = new HttpRest4jServiceImpl();
        String sourceName = this.getClass().getName();
        final FlowControlResult flowControlResult = new FlowControlResult();
        httpRest4jService.onBefore(sourceName, new HttpRequestEntity(), flowControlResult);
        httpRest4jService.onThrow(sourceName, new Exception("error"));
        httpRest4jService.onAfter(sourceName, new Object());
    }

    @After
    public void clear() {
        pluginConfigManagerMockedStatic.close();
        operationManagerMockedStatic.close();
        ChainContext.remove();
    }
}
